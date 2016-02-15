/*
 * MIT Kerberos KDC database backend for FreeIPA
 *
 * Authors: Simo Sorce <ssorce@redhat.com>
 *
 * Copyright (C) 2011  Simo Sorce, Red Hat
 * see file 'COPYING' for use and warranty information
 *
 * This program is free software you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

#include "ipa_kdb.h"

/*
 * During TGS request search by ipaKrbPrincipalName (case-insensitive)
 * and krbPrincipalName (case-sensitive)
 */
#define PRINC_TGS_SEARCH_FILTER "(&(|(objectclass=krbprincipalaux)" \
                                    "(objectclass=krbprincipal)" \
                                    "(objectclass=ipakrbprincipal))" \
                                    "(|(ipakrbprincipalalias=%s)" \
                                      "(krbprincipalname=%s)))"

#define PRINC_SEARCH_FILTER "(&(|(objectclass=krbprincipalaux)" \
                                "(objectclass=krbprincipal))" \
                              "(krbprincipalname=%s))"

static char *std_principal_attrs[] = {
    "krbPrincipalName",
    "krbCanonicalName",
    "ipaKrbPrincipalAlias",
    "krbUPEnabled",
    "krbPrincipalKey",
    "krbTicketPolicyReference",
    "krbPrincipalExpiration",
    "krbPasswordExpiration",
    "krbPwdPolicyReference",
    "krbPrincipalType",
    "krbPwdHistory",
    "krbLastPwdChange",
    "krbPrincipalAliases",
    "krbLastSuccessfulAuth",
    "krbLastFailedAuth",
    "krbLoginFailedCount",
    "krbExtraData",
    "krbLastAdminUnlock",
    "krbObjectReferences",
    "krbTicketFlags",
    "krbMaxTicketLife",
    "krbMaxRenewableAge",

    /* IPA SPECIFIC ATTRIBUTES */
    "nsaccountlock",
    "passwordHistory",

    "objectClass",
    NULL
};

static char *std_tktpolicy_attrs[] = {
    "krbmaxticketlife",
    "krbmaxrenewableage",
    "krbticketflags",

    NULL
};

#define TKTFLAGS_BIT        0x01
#define MAXTKTLIFE_BIT      0x02
#define MAXRENEWABLEAGE_BIT 0x04

static char *std_principal_obj_classes[] = {
    "krbprincipal",
    "krbprincipalaux",
    "krbTicketPolicyAux",
    "ipakrbprincipal",

    NULL
};

#define STD_PRINCIPAL_OBJ_CLASSES_SIZE (sizeof(std_principal_obj_classes) / sizeof(char *) - 1)

static int ipadb_ldap_attr_to_tl_data(LDAP *lcontext, LDAPMessage *le,
                                      char *attrname,
                                      krb5_tl_data **result, int *num)
{
    struct berval **vals;
    krb5_tl_data *prev, *next;
    krb5_int16 be_type;
    int i;
    int ret = ENOENT;

    *result = NULL;
    prev = NULL;
    next = NULL;
    vals = ldap_get_values_len(lcontext, le, attrname);
    if (vals) {
        for (i = 0; vals[i]; i++) {
            next = calloc(1, sizeof(krb5_tl_data));
            if (!next) {
                ret = ENOMEM;
                goto done;
            }

            /* fill tl_data struct with the data */
            memcpy(&be_type, vals[i]->bv_val, 2);
            next->tl_data_type = ntohs(be_type);
            next->tl_data_length = vals[i]->bv_len - 2;
            next->tl_data_contents = malloc(next->tl_data_length);
            if (!next->tl_data_contents) {
                ret = ENOMEM;
                goto done;
            }
            memcpy(next->tl_data_contents,
                   vals[i]->bv_val + 2,
                   next->tl_data_length);

            if (prev) {
                prev->tl_data_next = next;
            } else {
                *result = next;
            }
            prev = next;
        }
        *num = i;
        ret = 0;

        ldap_value_free_len(vals);
    }

done:
    if (ret) {
        free(next);
        if (*result) {
            prev = *result;
            while (prev) {
                next = prev->tl_data_next;
                free(prev);
                prev = next;
            }
        }
        *result = NULL;
        *num = 0;
    }
    return ret;
}

static krb5_error_code ipadb_set_tl_data(krb5_db_entry *entry,
                                         krb5_int16 type,
                                         krb5_ui_2 length,
                                         krb5_octet *data)
{
    krb5_error_code kerr;
    krb5_tl_data *new_td = NULL;
    krb5_tl_data *td;

    for (td = entry->tl_data; td; td = td->tl_data_next) {
        if (td->tl_data_type == type) {
            break;
        }
    }
    if (!td) {
        /* an existing entry was not found, make new */
        new_td = malloc(sizeof(krb5_tl_data));
        if (!new_td) {
            kerr = ENOMEM;
            goto done;
        }
        td = new_td;
        td->tl_data_next = entry->tl_data;
        td->tl_data_type = type;
        entry->tl_data = td;
        entry->n_tl_data++;
    }
    td->tl_data_length = length;
    td->tl_data_contents = malloc(td->tl_data_length);
    if (!td->tl_data_contents) {
        kerr = ENOMEM;
        goto done;
    }
    memcpy(td->tl_data_contents, data, td->tl_data_length);

    new_td = NULL;
    kerr = 0;

done:
    free(new_td);
    return kerr;
}

static int ipadb_ldap_attr_to_key_data(LDAP *lcontext, LDAPMessage *le,
                                       char *attrname,
                                       krb5_key_data **result, int *num,
                                       krb5_kvno *res_mkvno)
{
    struct berval **vals;
    int mkvno;
    int ret;

    vals = ldap_get_values_len(lcontext, le, attrname);
    if (!vals) {
        return ENOENT;
    }

    ret = ber_decode_krb5_key_data(vals[0], &mkvno, num, result);
    ldap_value_free_len(vals);
    if (ret == 0) {
        *res_mkvno = mkvno;
    }
    return ret;
}

static krb5_error_code ipadb_parse_ldap_entry(krb5_context kcontext,
                                              char *principal,
                                              LDAPMessage *lentry,
                                              krb5_db_entry **kentry,
                                              uint32_t *polmask)
{
    struct ipadb_context *ipactx;
    LDAP *lcontext;
    krb5_db_entry *entry;
    struct ipadb_e_data *ied;
    krb5_error_code kerr;
    krb5_tl_data *res_tl_data;
    krb5_key_data *res_key_data;
    krb5_kvno mkvno = 0;
    char **restrlist;
    char *restring;
    time_t restime;
    bool resbool;
    int result;
    int ret;

    *polmask = 0;
    entry = calloc(1, sizeof(krb5_db_entry));
    if (!entry) {
        return ENOMEM;
    }

    /* proceed to fill in attributes in the order they are defined in
     * krb5_db_entry in kdb.h */
    ipactx = ipadb_get_context(kcontext);
    if (!ipactx) {
        free(entry);
        return KRB5_KDB_DBNOTINITED;
    }
    lcontext = ipactx->lcontext;

    entry->magic = KRB5_KDB_MAGIC_NUMBER;
    entry->len = KRB5_KDB_V1_BASE_LENGTH;

    /* ignore mask for now */

    ret = ipadb_ldap_attr_to_int(lcontext, lentry,
                                 "krbTicketFlags", &result);
    if (ret == 0) {
        entry->attributes = result;
    } else {
        *polmask |= TKTFLAGS_BIT;
    }

    ret = ipadb_ldap_attr_to_int(lcontext, lentry,
                                 "krbMaxTicketLife", &result);
    if (ret == 0) {
        entry->max_life = result;
    } else {
        *polmask |= MAXTKTLIFE_BIT;
    }

    ret = ipadb_ldap_attr_to_int(lcontext, lentry,
                                 "krbMaxRenewableAge", &result);
    if (ret == 0) {
        entry->max_renewable_life = result;
    } else {
        *polmask |= MAXRENEWABLEAGE_BIT;
    }

    ret = ipadb_ldap_attr_to_time_t(lcontext, lentry,
                                    "krbPrincipalexpiration", &restime);
    switch (ret) {
    case 0:
        entry->expiration = restime;
    case ENOENT:
        break;
    default:
        kerr = KRB5_KDB_INTERNAL_ERROR;
        goto done;
    }

    ret = ipadb_ldap_attr_to_time_t(lcontext, lentry,
                                    "krbPasswordExpiration", &restime);
    switch (ret) {
    case 0:
        entry->pw_expiration = restime;
    case ENOENT:
        break;
    default:
        kerr = KRB5_KDB_INTERNAL_ERROR;
        goto done;
    }

    ret = ipadb_ldap_attr_to_time_t(lcontext, lentry,
                                    "krbLastSuccessfulAuth", &restime);
    switch (ret) {
    case 0:
        entry->last_success = restime;
    case ENOENT:
        break;
    default:
        kerr = KRB5_KDB_INTERNAL_ERROR;
        goto done;
    }

    ret = ipadb_ldap_attr_to_time_t(lcontext, lentry,
                                    "krbLastFailedAuth", &restime);
    switch (ret) {
    case 0:
        entry->last_failed = restime;
    case ENOENT:
        break;
    default:
        kerr = KRB5_KDB_INTERNAL_ERROR;
        goto done;
    }

    ret = ipadb_ldap_attr_to_int(lcontext, lentry,
                                 "krbLoginFailedCount", &result);
    if (ret == 0) {
        entry->fail_auth_count = result;
    }

    /* TODO: e_length, e_data */

    if (principal) {
        kerr = krb5_parse_name(kcontext, principal, &entry->princ);
        if (kerr != 0) {
            goto done;
        }
    } else {
        /* see if canonical name is available */
        ret = ipadb_ldap_attr_to_str(lcontext, lentry,
                                     "krbCanonicalName", &restring);
        switch (ret) {
        case ENOENT:
            /* if not pick the first principal name in the entry */
            ret = ipadb_ldap_attr_to_str(lcontext, lentry,
                                         "krbPrincipalName", &restring);
            if (ret != 0) {
                kerr = KRB5_KDB_INTERNAL_ERROR;
                goto done;
            }
        case 0:
            break;
        default:
            kerr = KRB5_KDB_INTERNAL_ERROR;
            goto done;
        }
        kerr = krb5_parse_name(kcontext, restring, &entry->princ);
        free(restring);
        if (kerr != 0) {
            goto done;
        }
    }

    ret = ipadb_ldap_attr_to_tl_data(lcontext, lentry,
                                     "krbExtraData", &res_tl_data, &result);
    switch (ret) {
    case 0:
        entry->tl_data = res_tl_data;
        entry->n_tl_data = result;
    case ENOENT:
        break;
    default:
        kerr = KRB5_KDB_INTERNAL_ERROR;
        goto done;
    }

    ret = ipadb_ldap_attr_to_key_data(lcontext, lentry,
                                      "krbPrincipalKey",
                                      &res_key_data, &result, &mkvno);
    switch (ret) {
    case 0:
        entry->key_data = res_key_data;
        entry->n_key_data = result;
        if (mkvno) {
            krb5_int16 kvno16le = htole16((krb5_int16)mkvno);

            kerr = ipadb_set_tl_data(entry, KRB5_TL_MKVNO,
                                     sizeof(kvno16le),
                                     (krb5_octet *)&kvno16le);
            if (kerr) {
                goto done;
            }
        }
    case ENOENT:
        break;
    default:
        kerr = KRB5_KDB_INTERNAL_ERROR;
        goto done;
    }

    ret = ipadb_ldap_attr_to_bool(lcontext, lentry,
                                  "nsAccountLock", &resbool);
    if ((ret == 0 && resbool == true) || (ret != 0 && ret != ENOENT)) {
        entry->attributes |= KRB5_KDB_DISALLOW_ALL_TIX;
    }

    ied = calloc(1, sizeof(struct ipadb_e_data));
    if (!ied) {
        kerr = ENOMEM;
        goto done;
    }
    ied->magic = IPA_E_DATA_MAGIC;

    entry->e_data = (krb5_octet *)ied;

    ied->entry_dn = ldap_get_dn(lcontext, lentry);
    if (!ied->entry_dn) {
        kerr = ENOMEM;
        goto done;
    }

    /* mark this as an ipa_user if it has the posixaccount objectclass */
    ret = ipadb_ldap_attr_has_value(lcontext, lentry,
                                    "objectClass", "posixAccount");
    if (ret != 0 && ret != ENOENT) {
        kerr = ret;
        goto done;
    }
    if (ret == 0) {
        ied->ipa_user = true;
    }

    ret = ipadb_ldap_attr_to_str(lcontext, lentry,
                                 "krbPwdPolicyReference", &restring);
    switch (ret) {
    case ENOENT:
        /* use the default policy if ref. is not available */
        ret = asprintf(&restring,
                       "cn=global_policy,%s", ipactx->realm_base);
        if (ret == -1) {
            kerr = ENOMEM;
            goto done;
        }
    case 0:
        break;
    default:
        kerr = KRB5_KDB_INTERNAL_ERROR;
        goto done;
    }
    ied->pw_policy_dn = restring;

    ret = ipadb_ldap_attr_to_strlist(lcontext, lentry,
                                     "passwordHistory", &restrlist);
    if (ret != 0 && ret != ENOENT) {
        kerr = KRB5_KDB_INTERNAL_ERROR;
        goto done;
    }
    if (ret == 0) {
        ied->pw_history = restrlist;
    }

    ret = ipadb_ldap_attr_to_time_t(lcontext, lentry,
                                    "krbLastPwdChange", &restime);
    if (ret == 0) {
        krb5_int32 time32le = htole32((krb5_int32)restime);

        kerr = ipadb_set_tl_data(entry,
                                 KRB5_TL_LAST_PWD_CHANGE,
                                 sizeof(time32le),
                                 (krb5_octet *)&time32le);
        if (kerr) {
            goto done;
        }

        ied->last_pwd_change = restime;
    }

    ret = ipadb_ldap_attr_to_time_t(lcontext, lentry,
                                    "krbLastAdminUnlock", &restime);
    if (ret == 0) {
        krb5_int32 time32le = htole32((krb5_int32)restime);

        kerr = ipadb_set_tl_data(entry,
                                 KRB5_TL_LAST_ADMIN_UNLOCK,
                                 sizeof(time32le),
                                 (krb5_octet *)&time32le);
        if (kerr) {
            goto done;
        }

        ied->last_admin_unlock = restime;
    }

    kerr = 0;

done:
    if (kerr) {
        ipadb_free_principal(kcontext, entry);
        entry = NULL;
    }
    *kentry = entry;
    return kerr;
}

static krb5_error_code ipadb_fetch_principals(struct ipadb_context *ipactx,
                                              unsigned int flags,
                                              char *principal,
                                              LDAPMessage **result)
{
    krb5_error_code kerr;
    char *src_filter = NULL;
    char *esc_original_princ = NULL;
    int ret, i;

    if (!ipactx->lcontext) {
        ret = ipadb_get_connection(ipactx);
        if (ret != 0) {
            kerr = KRB5_KDB_SERVER_INTERNAL_ERR;
            goto done;
        }
    }

    /* escape filter but do not touch '*' as this function accepts
     * wildcards in names */
    esc_original_princ = ipadb_filter_escape(principal, false);
    if (!esc_original_princ) {
        kerr = KRB5_KDB_INTERNAL_ERROR;
        goto done;
    }

    if (flags & KRB5_KDB_FLAG_ALIAS_OK) {
        ret = asprintf(&src_filter, PRINC_TGS_SEARCH_FILTER,
                       esc_original_princ, esc_original_princ);
    } else {
        ret = asprintf(&src_filter, PRINC_SEARCH_FILTER, esc_original_princ);
    }

    if (ret == -1) {
        kerr = KRB5_KDB_INTERNAL_ERROR;
        goto done;
    }

    kerr = ipadb_simple_search(ipactx,
                               ipactx->base, LDAP_SCOPE_SUBTREE,
                               src_filter, std_principal_attrs,
                               result);

done:
    free(src_filter);
    free(esc_original_princ);
    return kerr;
}

static krb5_error_code ipadb_find_principal(krb5_context kcontext,
                                            unsigned int flags,
                                            LDAPMessage *res,
                                            char **principal,
                                            LDAPMessage **entry)
{
    struct ipadb_context *ipactx;
    bool found = false;
    LDAPMessage *le = NULL;
    struct berval **vals;
    int i;

    ipactx = ipadb_get_context(kcontext);
    if (!ipactx) {
        return KRB5_KDB_DBNOTINITED;
    }

    while (!found) {

        if (!le) {
            le = ldap_first_entry(ipactx->lcontext, res);
        } else {
            le = ldap_next_entry(ipactx->lcontext, le);
        }
        if (!le) {
            break;
        }

        vals = ldap_get_values_len(ipactx->lcontext, le, "krbprincipalname");
        if (vals == NULL) {
            continue;
        }

        /* we need to check for a strict match as a '*' in the name may have
         * caused the ldap server to return multiple entries */
        for (i = 0; vals[i]; i++) {
            /* KDC will accept aliases when doing TGT lookup (ref_tgt_again in do_tgs_req.c */
            /* Use case-insensitive comparison in such cases */
            if ((flags & KRB5_KDB_FLAG_ALIAS_OK) != 0) {
                found = (strcasecmp(vals[i]->bv_val, (*principal)) == 0);
            } else {
                found = (strcmp(vals[i]->bv_val, (*principal)) == 0);
            }
        }

        ldap_value_free_len(vals);

        if (!found) {
            continue;
        }

        /* we need to check if this is the canonical name */
        vals = ldap_get_values_len(ipactx->lcontext, le, "krbcanonicalname");
        if (vals == NULL) {
            continue;
        }

        /* Again, if aliases are accepted by KDC, use case-insensitive comparison */
        if ((flags & KRB5_KDB_FLAG_ALIAS_OK) != 0) {
            found = (strcasecmp(vals[0]->bv_val, (*principal)) == 0);
        } else {
            found = (strcmp(vals[0]->bv_val, (*principal)) == 0);
        }

        if (!found) {
            /* search does not allow aliases */
            ldap_value_free_len(vals);
            continue;
        }

        free(*principal);
        *principal = strdup(vals[0]->bv_val);
        if (!(*principal)) {
            return KRB5_KDB_INTERNAL_ERROR;
        }

        ldap_value_free_len(vals);
    }

    if (!found || !le) {
        return KRB5_KDB_NOENTRY;
    }

    *entry = le;
    return 0;
}

static krb5_error_code ipadb_fetch_tktpolicy(krb5_context kcontext,
                                             LDAPMessage *lentry,
                                             krb5_db_entry *entry,
                                             uint32_t polmask)
{
    struct ipadb_context *ipactx;
    krb5_error_code kerr;
    char *policy_dn = NULL;
    LDAPMessage *res = NULL;
    LDAPMessage *first;
    int result;
    int ret;

    ipactx = ipadb_get_context(kcontext);
    if (!ipactx) {
        return KRB5_KDB_DBNOTINITED;
    }

    ret = ipadb_ldap_attr_to_str(ipactx->lcontext, lentry,
                                 "krbticketpolicyreference", &policy_dn);
    switch (ret) {
    case 0:
        break;
    case ENOENT:
        ret = asprintf(&policy_dn, "cn=%s,cn=kerberos,%s",
                                   ipactx->realm, ipactx->base);
        if (ret == -1) {
            kerr = ENOMEM;
            goto done;
        }
        break;
    default:
        kerr = ret;
        goto done;
    }

    kerr = ipadb_simple_search(ipactx,
                               policy_dn, LDAP_SCOPE_BASE,
                               "(objectclass=krbticketpolicyaux)",
                               std_tktpolicy_attrs,
                               &res);
    if (kerr == 0) {
        first = ldap_first_entry(ipactx->lcontext, res);
        if (!first) {
            kerr = KRB5_KDB_NOENTRY;
        } else {
            if (polmask & MAXTKTLIFE_BIT) {
                ret = ipadb_ldap_attr_to_int(ipactx->lcontext, first,
                                             "krbmaxticketlife", &result);
                if (ret == 0) {
                    entry->max_life = result;
                } else {
                    entry->max_life = 86400;
                }
            }
            if (polmask & MAXRENEWABLEAGE_BIT) {
                ret = ipadb_ldap_attr_to_int(ipactx->lcontext, first,
                                             "krbmaxrenewableage", &result);
                if (ret == 0) {
                    entry->max_renewable_life = result;
                } else {
                    entry->max_renewable_life = 604800;
                }
            }
            if (polmask & TKTFLAGS_BIT) {
                ret = ipadb_ldap_attr_to_int(ipactx->lcontext, first,
                                             "krbticketflags", &result);
                if (ret == 0) {
                    entry->attributes |= result;
                } else {
                    entry->attributes |= KRB5_KDB_REQUIRES_PRE_AUTH;
                }
            }
        }
    }

    if (kerr == KRB5_KDB_NOENTRY) {
        /* No policy at all ??
         * set hardcoded default policy for now */
        if (polmask & MAXTKTLIFE_BIT) {
            entry->max_life = 86400;
        }
        if (polmask & MAXRENEWABLEAGE_BIT) {
            entry->max_renewable_life = 604800;
        }
        if (polmask & TKTFLAGS_BIT) {
            entry->attributes |= KRB5_KDB_REQUIRES_PRE_AUTH;
        }

        kerr = 0;
    }

done:
    ldap_msgfree(res);
    free(policy_dn);
    return kerr;
}

/* TODO: handle case where main object and krbprincipal data are not
 * the same object but linked objects ?
 * (by way of krbprincipalaux being in a separate object from krbprincipal).
 * Currently we only support objcts with both objectclasses present at the
 * same time. */

krb5_error_code ipadb_get_principal(krb5_context kcontext,
                                    krb5_const_principal search_for,
                                    unsigned int flags,
                                    krb5_db_entry **entry)
{
    struct ipadb_context *ipactx;
    krb5_error_code kerr;
    char *principal = NULL;
    LDAPMessage *res = NULL;
    LDAPMessage *lentry;
    uint32_t pol;

    ipactx = ipadb_get_context(kcontext);
    if (!ipactx) {
        return KRB5_KDB_DBNOTINITED;
    }

    kerr = krb5_unparse_name(kcontext, search_for, &principal);
    if (kerr != 0) {
        goto done;
    }

    kerr = ipadb_fetch_principals(ipactx, flags, principal, &res);
    if (kerr != 0) {
        goto done;
    }

    kerr = ipadb_find_principal(kcontext, flags, res, &principal, &lentry);
    if (kerr != 0) {
        goto done;
    }

    kerr = ipadb_parse_ldap_entry(kcontext, principal, lentry, entry, &pol);
    if (kerr != 0) {
        goto done;
    }

    if (pol) {
        kerr = ipadb_fetch_tktpolicy(kcontext, lentry, *entry, pol);
        if (kerr != 0) {
            goto done;
        }
    }

done:
    ldap_msgfree(res);
    krb5_free_unparsed_name(kcontext, principal);
    return kerr;
}

void ipadb_free_principal(krb5_context kcontext, krb5_db_entry *entry)
{
    struct ipadb_e_data *ied;
    krb5_tl_data *prev, *next;
    int i;

    if (entry) {
        krb5_free_principal(kcontext, entry->princ);
        prev = entry->tl_data;
        while(prev) {
            next = prev->tl_data_next;
            free(prev->tl_data_contents);
            free(prev);
            prev = next;
        }
        ipa_krb5_free_key_data(entry->key_data, entry->n_key_data);

        if (entry->e_data) {
            ied = (struct ipadb_e_data *)entry->e_data;
            if (ied->magic == IPA_E_DATA_MAGIC) {
                ldap_memfree(ied->entry_dn);
                free(ied->passwd);
                free(ied->pw_policy_dn);
                for (i = 0; ied->pw_history && ied->pw_history[i]; i++) {
                    free(ied->pw_history[i]);
                }
                free(ied->pw_history);
                free(ied->pol);
                free(ied);
            }
        }

        free(entry);
    }
}

static krb5_error_code ipadb_get_tl_data(krb5_db_entry *entry,
                                         krb5_int16 type,
                                         krb5_ui_2 length,
                                         krb5_octet *data)
{
    krb5_tl_data *td;

    for (td = entry->tl_data; td; td = td->tl_data_next) {
        if (td->tl_data_type == type) {
            break;
        }
    }
    if (!td) {
        return ENOENT;
    }

    if (td->tl_data_length != length) {
        return EINVAL;
    }

    memcpy(data, td->tl_data_contents, length);

    return 0;
}

struct ipadb_mods {
    LDAPMod **mods;
    int alloc_size;
    int tip;
};

static int new_ipadb_mods(struct ipadb_mods **imods)
{
    struct ipadb_mods *r;

    r = malloc(sizeof(struct ipadb_mods));
    if (!r) {
        return ENOMEM;
    }

    /* alloc the average space for a full change of all ldap attrinbutes */
    r->alloc_size = 15;
    r->mods = calloc(r->alloc_size, sizeof(LDAPMod *));
    if (!r->mods) {
        free(r);
        return ENOMEM;
    }
    r->tip = 0;

    *imods = r;
    return 0;
}

static void ipadb_mods_free(struct ipadb_mods *imods)
{
    if (imods == NULL) {
        return;
    }

    ldap_mods_free(imods->mods, 1);
    free(imods);
}

static krb5_error_code ipadb_mods_new(struct ipadb_mods *imods,
                                      LDAPMod **slot)
{
    LDAPMod **lmods = NULL;
    LDAPMod *m;
    int n;

    lmods = imods->mods;
    for (n = imods->tip; n < imods->alloc_size && lmods[n] != NULL; n++) {
        /* find empty slot */ ;
    }

    if (n + 1 > imods->alloc_size) {
        /* need to increase size */
        lmods = realloc(imods->mods, (n * 2) * sizeof(LDAPMod *));
        if (!lmods) {
            return ENOMEM;
        }
        imods->mods = lmods;
        imods->alloc_size = n * 2;
        memset(&lmods[n + 1], 0,
               (imods->alloc_size - n - 1) * sizeof(LDAPMod *));
    }

    m = calloc(1, sizeof(LDAPMod));
    if (!m) {
        return ENOMEM;
    }
    imods->tip = n;
    *slot = imods->mods[n] = m;
    return 0;
}

static void ipadb_mods_free_tip(struct ipadb_mods *imods)
{
    LDAPMod *m;
    int i;

    if (imods->alloc_size == 0) {
        return;
    }

    m = imods->mods[imods->tip];

    if (!m) {
        return;
    }

    free(m->mod_type);
    if (m->mod_values) {
        for (i = 0; m->mod_values[i]; i++) {
            free(m->mod_values[i]);
        }
    }
    free(m->mod_values);
    free(m);

    imods->mods[imods->tip] = NULL;
    imods->tip--;
}

static krb5_error_code ipadb_get_ldap_mod_str(struct ipadb_mods *imods,
                                              char *attribute, char *value,
                                              int mod_op)
{
    krb5_error_code kerr;
    LDAPMod *m = NULL;

    kerr = ipadb_mods_new(imods, &m);
    if (kerr) {
        return kerr;
    }

    m->mod_op = mod_op;
    m->mod_type = strdup(attribute);
    if (!m->mod_type) {
        kerr = ENOMEM;
        goto done;
    }
    m->mod_values = calloc(2, sizeof(char *));
    if (!m->mod_values) {
        kerr = ENOMEM;
        goto done;
    }
    m->mod_values[0] = strdup(value);
    if (!m->mod_values[0]) {
        kerr = ENOMEM;
        goto done;
    }

    kerr = 0;

done:
    if (kerr) {
        ipadb_mods_free_tip(imods);
    }
    return kerr;
}

static krb5_error_code ipadb_get_ldap_mod_int(struct ipadb_mods *imods,
                                              char *attribute, int value,
                                              int mod_op)
{
    krb5_error_code kerr;
    char *v = NULL;
    int ret;

    ret = asprintf(&v, "%d", value);
    if (ret == -1) {
        kerr = KRB5_KDB_INTERNAL_ERROR;
        goto done;
    }

    kerr = ipadb_get_ldap_mod_str(imods, attribute, v, mod_op);

done:
    free(v);
    return kerr;
}

static krb5_error_code ipadb_get_ldap_mod_time(struct ipadb_mods *imods,
                                               char *attribute,
                                               krb5_timestamp value,
                                               int mod_op)
{
    struct tm date, *t;
    time_t timeval;
    char v[20];

    timeval = (time_t)value;
    t = gmtime_r(&timeval, &date);
    if (t == NULL) {
        return EINVAL;
    }

    strftime(v, 20, "%Y%m%d%H%M%SZ", &date);

    return ipadb_get_ldap_mod_str(imods, attribute, v, mod_op);
}

static krb5_error_code ipadb_get_ldap_mod_bvalues(struct ipadb_mods *imods,
                                                  char *attribute,
                                                  struct berval **values,
                                                  int num_values,
                                                  int mod_op)
{
    krb5_error_code kerr;
    LDAPMod *m = NULL;
    int i;

    if (values == NULL || values[0] == NULL || num_values <= 0) {
        return EINVAL;
    }

    kerr = ipadb_mods_new(imods, &m);
    if (kerr) {
        return kerr;
    }

    m->mod_op = mod_op | LDAP_MOD_BVALUES;
    m->mod_type = strdup(attribute);
    if (!m->mod_type) {
        kerr = ENOMEM;
        goto done;
    }
    m->mod_bvalues = calloc(num_values + 1, sizeof(struct berval *));
    if (!m->mod_bvalues) {
        kerr = ENOMEM;
        goto done;
    }

    for (i = 0; i < num_values; i++) {
        m->mod_bvalues[i] = values[i];
    }

    kerr = 0;

done:
    if (kerr) {
        /* we need to free bvalues manually here otherwise
         * ipadb_mods_free_tip will free contents which we
         * did not allocate here */
        free(m->mod_bvalues);
        m->mod_bvalues = NULL;
        ipadb_mods_free_tip(imods);
    }
    return kerr;
}

static krb5_error_code ipadb_get_ldap_mod_extra_data(struct ipadb_mods *imods,
                                                     krb5_tl_data *tl_data,
                                                     int mod_op)
{
    krb5_error_code kerr;
    krb5_tl_data *data;
    struct berval **bvs = NULL;
    krb5_int16 be_type;
    int n, i;

    for (n = 0, data = tl_data; data; data = data->tl_data_next) {
        if (data->tl_data_type == KRB5_TL_LAST_PWD_CHANGE ||
            data->tl_data_type == KRB5_TL_KADM_DATA ||
            data->tl_data_type == KRB5_TL_DB_ARGS ||
            data->tl_data_type == KRB5_TL_MKVNO ||
            data->tl_data_type == KRB5_TL_LAST_ADMIN_UNLOCK) {
            continue;
        }
        n++;
    }

    if (n == 0) {
        return ENOENT;
    }

    bvs = calloc(n + 1, sizeof(struct berval *));
    if (!bvs) {
        kerr = ENOMEM;
        goto done;
    }

    for (i = 0, data = tl_data; data; data = data->tl_data_next) {

        if (data->tl_data_type == KRB5_TL_LAST_PWD_CHANGE ||
            data->tl_data_type == KRB5_TL_KADM_DATA ||
            data->tl_data_type == KRB5_TL_DB_ARGS ||
            data->tl_data_type == KRB5_TL_MKVNO ||
            data->tl_data_type == KRB5_TL_LAST_ADMIN_UNLOCK) {
            continue;
        }

        be_type = htons(data->tl_data_type);

        bvs[i] = calloc(1, sizeof(struct berval));
        if (!bvs[i]) {
            kerr = ENOMEM;
            goto done;
        }

        bvs[i]->bv_len = data->tl_data_length + 2;
        bvs[i]->bv_val = malloc(bvs[i]->bv_len);
        if (!bvs[i]->bv_val) {
            kerr = ENOMEM;
            goto done;
        }
        memcpy(bvs[i]->bv_val, &be_type, 2);
        memcpy(&(bvs[i]->bv_val[2]), data->tl_data_contents, data->tl_data_length);

        i++;

        if (i > n) {
            kerr = KRB5_KDB_INTERNAL_ERROR;
            goto done;
        }
    }

    kerr = ipadb_get_ldap_mod_bvalues(imods, "krbExtraData", bvs, i, mod_op);

done:
    if (kerr) {
        for (i = 0; bvs && bvs[i]; i++) {
            free(bvs[i]->bv_val);
            free(bvs[i]);
        }
    }
    free(bvs);
    return kerr;
}

static krb5_error_code ipadb_get_mkvno_from_tl_data(krb5_tl_data *tl_data,
                                                    int *mkvno)
{
    krb5_tl_data *data;
    int master_kvno = 0;
    krb5_int16 tmp;

    for (data = tl_data; data; data = data->tl_data_next) {

        if (data->tl_data_type != KRB5_TL_MKVNO) {
            continue;
        }

        if (data->tl_data_length != 2) {
            return KRB5_KDB_TRUNCATED_RECORD;
        }

        memcpy(&tmp, data->tl_data_contents, 2);
        master_kvno = le16toh(tmp);

        break;
    }

    if (master_kvno == 0) {
        /* fall back to std mkvno of 1 */
        *mkvno = 1;
    } else {
        *mkvno = master_kvno;
    }

    return 0;
}

static krb5_error_code ipadb_get_ldap_mod_key_data(struct ipadb_mods *imods,
                                                   krb5_key_data *key_data,
                                                   int n_key_data, int mkvno,
                                                   int mod_op)
{
    krb5_error_code kerr;
    struct berval *bval = NULL;
    int ret;

    ret = ber_encode_krb5_key_data(key_data, n_key_data, mkvno, &bval);
    if (ret != 0) {
        kerr = ret;
        goto done;
    }

    kerr = ipadb_get_ldap_mod_bvalues(imods, "krbPrincipalKey",
                                      &bval, 1, mod_op);

done:
    if (kerr) {
        ber_bvfree(bval);
    }
    return kerr;
}

static krb5_error_code ipadb_get_ldap_mod_str_list(struct ipadb_mods *imods,
                                                   char *attrname,
                                                   char **strlist, int len,
                                                   int mod_op)
{
    krb5_error_code kerr;
    struct berval **bvs = NULL;
    int i;

    bvs = calloc(len + 1, sizeof(struct berval *));
    if (!bvs) {
        kerr = ENOMEM;
        goto done;
    }

    for (i = 0; i < len; i++) {
        bvs[i] = calloc(1, sizeof(struct berval));
        if (!bvs[i]) {
            kerr = ENOMEM;
            goto done;
        }

        bvs[i]->bv_val = strdup(strlist[i]);
        if (!bvs[i]->bv_val) {
            kerr = ENOMEM;
            goto done;
        }
        bvs[i]->bv_len = strlen(strlist[i]) + 1;
    }

    kerr = ipadb_get_ldap_mod_bvalues(imods, attrname, bvs, len, mod_op);

done:
    if (kerr) {
        for (i = 0; bvs && bvs[i]; i++) {
            free(bvs[i]->bv_val);
            free(bvs[i]);
        }
    }
    free(bvs);
    return kerr;
}

static krb5_error_code ipadb_entry_to_mods(krb5_context kcontext,
                                           struct ipadb_mods *imods,
                                           krb5_db_entry *entry,
                                           char *principal,
                                           int mod_op)
{
    krb5_error_code kerr;
    krb5_int32 time32le;
    int mkvno;

    /* check each mask flag in order */

    /* KADM5_PRINCIPAL */
    if (entry->mask & KMASK_PRINCIPAL) {
        kerr = ipadb_get_ldap_mod_str(imods, "krbPrincipalName",
                                      principal, mod_op);
        if (kerr) {
            goto done;
        }
        kerr = ipadb_get_ldap_mod_str(imods, "ipaKrbPrincipalAlias",
                                      principal, mod_op);
        if (kerr) {
            goto done;
        }
    }

    /* KADM5_PRINC_EXPIRE_TIME */
    if (entry->mask & KMASK_PRINC_EXPIRE_TIME) {
        kerr = ipadb_get_ldap_mod_time(imods,
                                       "krbPrincipalExpiration",
                                       entry->expiration,
                                       mod_op);
        if (kerr) {
            goto done;
        }
    }

    /* KADM5_PW_EXPIRATION */
    if (entry->mask & KMASK_PW_EXPIRATION) {
        kerr = ipadb_get_ldap_mod_time(imods,
                                       "krbPasswordExpiration",
                                       entry->pw_expiration,
                                       mod_op);
        if (kerr) {
            goto done;
        }
    }

    /* KADM5_LAST_PWD_CHANGE */
    /* apparently, at least some versions of kadmin fail to set this flag
     * when they do include a pwd change timestamp in TL_DATA.
     * So for now check if KADM5_KEY_DATA has been set, which kadm5
     * always does on password changes */
#if KADM5_ACTUALLY_SETS_LAST_PWD_CHANGE
    if (entry->mask & KMASK_LAST_PWD_CHANGE) {
        if (!entry->n_tl_data) {
            kerr = EINVAL;
            goto done;
        }

#else
    if (entry->n_tl_data &&
        entry->mask & KMASK_KEY_DATA) {
#endif
        kerr = ipadb_get_tl_data(entry,
                                 KRB5_TL_LAST_PWD_CHANGE,
                                 sizeof(time32le),
                                 (krb5_octet *)&time32le);
        if (kerr && kerr != ENOENT) {
            goto done;
        }
        if (kerr == 0) {
            kerr = ipadb_get_ldap_mod_time(imods,
                                           "krbLastPwdChange",
                                           le32toh(time32le),
                                           mod_op);
            if (kerr) {
                goto done;
            }
        }
    }

    /* KADM5_ATTRIBUTES */
    if (entry->mask & KMASK_ATTRIBUTES) {
        kerr = ipadb_get_ldap_mod_int(imods,
                                      "krbTicketFlags",
                                      (int)entry->attributes,
                                      mod_op);
        if (kerr) {
            goto done;
        }
    }

    /* KADM5_MAX_LIFE */
    if (entry->mask & KMASK_MAX_LIFE) {
        kerr = ipadb_get_ldap_mod_int(imods,
                                      "krbMaxTicketLife",
                                      (int)entry->max_life,
                                      mod_op);
        if (kerr) {
            goto done;
        }
    }

    /* KADM5_MOD_TIME */
    /* KADM5_MOD_NAME */
    /* KADM5_KVNO */
    /* KADM5_MKVNO */
    /* KADM5_AUX_ATTRIBUTES */
    /* KADM5_POLICY */
    /* KADM5_POLICY_CLR */

    /* version 2 masks */
    /* KADM5_MAX_RLIFE */
    if (entry->mask & KMASK_MAX_RLIFE) {
        kerr = ipadb_get_ldap_mod_int(imods,
                                      "krbMaxRenewableAge",
                                      (int)entry->max_renewable_life,
                                      mod_op);
        if (kerr) {
            goto done;
        }
    }

    /* KADM5_LAST_SUCCESS */
    if (entry->mask & KMASK_LAST_SUCCESS) {
        kerr = ipadb_get_ldap_mod_time(imods,
                                       "krbLastSuccessfulAuth",
                                       entry->last_success,
                                       mod_op);
        if (kerr) {
            goto done;
        }
    }

    /* KADM5_LAST_FAILED */
    if (entry->mask & KMASK_LAST_FAILED) {
        kerr = ipadb_get_ldap_mod_time(imods,
                                       "krbLastFailedAuth",
                                       entry->last_failed,
                                       mod_op);
        if (kerr) {
            goto done;
        }
    }

    /* KADM5_FAIL_AUTH_COUNT */
    if (entry->mask & KMASK_FAIL_AUTH_COUNT) {
        kerr = ipadb_get_ldap_mod_int(imods,
                                      "krbLoginFailedCount",
                                      (int)entry->fail_auth_count,
                                      mod_op);
        if (kerr) {
            goto done;
        }
    }

    /* KADM5_KEY_DATA */
    if (entry->mask & KMASK_KEY_DATA) {
        /* TODO: password changes should go via change_pwd
         * then we can get clear text and set all needed
         * LDAP attributes */

        kerr = ipadb_get_mkvno_from_tl_data(entry->tl_data, &mkvno);
        if (kerr) {
            goto done;
        }

        kerr = ipadb_get_ldap_mod_key_data(imods,
                                           entry->key_data,
                                           entry->n_key_data,
                                           mkvno,
                                           mod_op);
        if (kerr) {
            goto done;
        }
    }

    /* KADM5_TL_DATA */
    if (entry->mask & KMASK_TL_DATA) {
        kerr = ipadb_get_tl_data(entry,
                                 KRB5_TL_LAST_ADMIN_UNLOCK,
                                 sizeof(time32le),
                                 (krb5_octet *)&time32le);
        if (kerr && kerr != ENOENT) {
            goto done;
        }
        if (kerr == 0) {
            kerr = ipadb_get_ldap_mod_time(imods,
                                           "krbLastAdminUnlock",
                                           le32toh(time32le),
                                           mod_op);
            if (kerr) {
                goto done;
            }
        }

        kerr = ipadb_get_ldap_mod_extra_data(imods,
                                             entry->tl_data,
                                             mod_op);
        if (kerr && kerr != ENOENT) {
            goto done;
        }
    }

    /* KADM5_LOAD */

    /* Handle password change related operations. */
    if (entry->e_data) {
        struct ipadb_e_data *ied;
        time_t now = time(NULL);
        time_t expire_time;
        char **new_history;
        int nh_len;
        int ret;
        int i;

        ied = (struct ipadb_e_data *)entry->e_data;
        if (ied->magic != IPA_E_DATA_MAGIC) {
            kerr = EINVAL;
            goto done;
        }

        /*
         * We need to set userPassword and history only if this is
         * a IPA User, we don't do that for simple service principals
         */
        if (ied->ipa_user && ied->passwd) {
            kerr = ipadb_get_ldap_mod_str(imods, "userPassword",
                                          ied->passwd, mod_op);
            if (kerr) {
                goto done;
            }

            /* Also set new password expiration time.
             * Have to do it here because kadmin doesn't know policies and
             * resets entry->mask after we have gone through the password
             * change code.  */
            kerr = ipadb_get_pwd_expiration(kcontext, entry,
                                            ied, &expire_time);
            if (kerr) {
                goto done;
            }

            kerr = ipadb_get_ldap_mod_time(imods,
                                           "krbPasswordExpiration",
                                           expire_time, mod_op);
            if (kerr) {
                goto done;
            }
        }

        if (ied->ipa_user && ied->passwd &&
            ied->pol && ied->pol->history_length) {
            ret = ipapwd_generate_new_history(ied->passwd, now,
                                              ied->pol->history_length,
                                              ied->pw_history,
                                              &new_history, &nh_len);
            if (ret) {
                kerr = ret;
                goto done;
            }

            kerr = ipadb_get_ldap_mod_str_list(imods, "passwordHistory",
                                               new_history, nh_len, mod_op);

            for (i = 0; i < nh_len; i++) {
                free(new_history[i]);
            }
            free(new_history);

            if (kerr) {
                goto done;
            }
        }
    }

    kerr = 0;

done:
    return kerr;
}

/* adds default objectclasses and attributes */
static krb5_error_code ipadb_entry_default_attrs(struct ipadb_mods *imods)
{
    krb5_error_code kerr;
    LDAPMod *m = NULL;
    int i;

    kerr = ipadb_mods_new(imods, &m);
    if (kerr) {
        return kerr;
    }

    m->mod_op = LDAP_MOD_ADD;
    m->mod_type = strdup("objectClass");
    if (!m->mod_type) {
        kerr = ENOMEM;
        goto done;
    }
    m->mod_values = calloc(STD_PRINCIPAL_OBJ_CLASSES_SIZE + 1, sizeof(char *));
    if (!m->mod_values) {
        kerr = ENOMEM;
        goto done;
    }
    for (i = 0; i < STD_PRINCIPAL_OBJ_CLASSES_SIZE; i++) {
        m->mod_values[i] = strdup(std_principal_obj_classes[i]);
        if (!m->mod_values[i]) {
            kerr = ENOMEM;
            goto done;
        }
    }

    kerr = 0;

done:
    if (kerr) {
        ipadb_mods_free_tip(imods);
    }
    return kerr;
}

static krb5_error_code ipadb_add_principal(krb5_context kcontext,
                                           krb5_db_entry *entry)
{
    struct ipadb_context *ipactx;
    krb5_error_code kerr;
    char *principal = NULL;
    struct ipadb_mods *imods = NULL;
    char *dn = NULL;
    int ret;

    ipactx = ipadb_get_context(kcontext);
    if (!ipactx) {
        kerr = KRB5_KDB_DBNOTINITED;
        goto done;
    }

    if (!ipactx->override_restrictions) {
        return KRB5_KDB_CONSTRAINT_VIOLATION;
    }

    kerr = krb5_unparse_name(kcontext, entry->princ, &principal);
    if (kerr != 0) {
        goto done;
    }

    ret = asprintf(&dn, "krbPrincipalName=%s,cn=%s,cn=kerberos,%s",
                        principal, ipactx->realm, ipactx->base);
    if (ret == -1) {
        kerr = ENOMEM;
        goto done;
    }

    ret = new_ipadb_mods(&imods);
    if (ret != 0) {
        kerr = ret;
        goto done;
    }

    kerr = ipadb_entry_default_attrs(imods);
    if (kerr != 0) {
        goto done;
    }

    kerr = ipadb_entry_to_mods(kcontext, imods,
                               entry, principal, LDAP_MOD_ADD);
    if (kerr != 0) {
        goto done;
    }

    kerr = ipadb_simple_add(ipactx, dn, imods->mods);

done:
    ipadb_mods_free(imods);
    krb5_free_unparsed_name(kcontext, principal);
    ldap_memfree(dn);
    return kerr;
}

static krb5_error_code ipadb_modify_principal(krb5_context kcontext,
                                              krb5_db_entry *entry)
{
    struct ipadb_context *ipactx;
    krb5_error_code kerr;
    char *principal = NULL;
    LDAPMessage *res = NULL;
    LDAPMessage *lentry;
    struct ipadb_mods *imods = NULL;
    char *dn = NULL;
    struct ipadb_e_data *ied;

    ipactx = ipadb_get_context(kcontext);
    if (!ipactx) {
        return KRB5_KDB_DBNOTINITED;
    }

    ied = (struct ipadb_e_data *)entry->e_data;
    if (!ied || !ied->entry_dn) {
        kerr = krb5_unparse_name(kcontext, entry->princ, &principal);
        if (kerr != 0) {
            goto done;
        }

        kerr = ipadb_fetch_principals(ipactx, 0, principal, &res);
        if (kerr != 0) {
            goto done;
        }

        /* FIXME: no alias allowed for now, should we allow modifies
         * by alias name ? */
        kerr = ipadb_find_principal(kcontext, 0, res, &principal, &lentry);
        if (kerr != 0) {
            goto done;
        }

        dn = ldap_get_dn(ipactx->lcontext, lentry);
        if (!dn) {
            kerr = KRB5_KDB_INTERNAL_ERROR;
            goto done;
        }
    }

    kerr = new_ipadb_mods(&imods);
    if (kerr) {
        goto done;
    }

    kerr = ipadb_entry_to_mods(kcontext, imods,
                               entry, principal, LDAP_MOD_REPLACE);
    if (kerr != 0) {
        goto done;
    }

    if (!ied || !ied->entry_dn) {
        kerr = ipadb_simple_modify(ipactx, dn, imods->mods);
    } else {
        kerr = ipadb_simple_modify(ipactx, ied->entry_dn, imods->mods);
    }

done:
    ipadb_mods_free(imods);
    ldap_msgfree(res);
    krb5_free_unparsed_name(kcontext, principal);
    ldap_memfree(dn);
    return kerr;
}

krb5_error_code ipadb_put_principal(krb5_context kcontext,
                                    krb5_db_entry *entry,
                                    char **db_args)
{
    if (entry->mask & KMASK_PRINCIPAL) {
        return ipadb_add_principal(kcontext, entry);
    } else {
        return ipadb_modify_principal(kcontext, entry);
    }
}

static krb5_error_code ipadb_delete_entry(krb5_context kcontext,
                                          LDAPMessage *lentry)
{
    struct ipadb_context *ipactx;
    krb5_error_code kerr;
    char *dn = NULL;
    int ret;

    ipactx = ipadb_get_context(kcontext);
    if (!ipactx) {
        kerr = KRB5_KDB_DBNOTINITED;
        goto done;
    }

    if (!ipactx->lcontext) {
        ret = ipadb_get_connection(ipactx);
        if (ret != 0) {
            kerr = KRB5_KDB_SERVER_INTERNAL_ERR;
            goto done;
        }
    }

    dn = ldap_get_dn(ipactx->lcontext, lentry);
    if (!dn) {
        kerr = KRB5_KDB_INTERNAL_ERROR;
        goto done;
    }

    kerr = ipadb_simple_delete(ipactx, dn);

done:
    ldap_memfree(dn);
    return kerr;
}

static krb5_error_code ipadb_delete_alias(krb5_context kcontext,
                                          LDAPMessage *lentry,
                                          char *principal)
{
    struct ipadb_context *ipactx;
    krb5_error_code kerr;
    char *dn = NULL;
    int ret;

    ipactx = ipadb_get_context(kcontext);
    if (!ipactx) {
        kerr = KRB5_KDB_DBNOTINITED;
        goto done;
    }

    if (!ipactx->lcontext) {
        ret = ipadb_get_connection(ipactx);
        if (ret != 0) {
            kerr = KRB5_KDB_SERVER_INTERNAL_ERR;
            goto done;
        }
    }

    dn = ldap_get_dn(ipactx->lcontext, lentry);
    if (!dn) {
        kerr = KRB5_KDB_INTERNAL_ERROR;
        goto done;
    }

    kerr = ipadb_simple_delete_val(ipactx, dn, "krbprincipalname", principal);

done:
    ldap_memfree(dn);
    return kerr;
}

krb5_error_code ipadb_delete_principal(krb5_context kcontext,
                                       krb5_const_principal search_for)
{
    struct ipadb_context *ipactx;
    krb5_error_code kerr;
    char *principal = NULL;
    char *canonicalized = NULL;
    LDAPMessage *res = NULL;
    LDAPMessage *lentry;
    unsigned int flags;

    ipactx = ipadb_get_context(kcontext);
    if (!ipactx) {
        return KRB5_KDB_DBNOTINITED;
    }

    if (!ipactx->override_restrictions) {
        return KRB5_KDB_CONSTRAINT_VIOLATION;
    }

    kerr = krb5_unparse_name(kcontext, search_for, &principal);
    if (kerr != 0) {
        goto done;
    }

    kerr = ipadb_fetch_principals(ipactx, 0, principal, &res);
    if (kerr != 0) {
        goto done;
    }

    canonicalized = strdup(principal);
    if (!canonicalized) {
        kerr = ENOMEM;
        goto done;
    }

    flags = KRB5_KDB_FLAG_ALIAS_OK;
    kerr = ipadb_find_principal(kcontext, flags, res, &canonicalized, &lentry);
    if (kerr != 0) {
        goto done;
    }

    /* check if this is an alias (remove it) or if we should remove the whole
     * ldap record */

    /* TODO: should we use case insensitive matching here ? */
    if (strcmp(canonicalized, principal) == 0) {
        kerr = ipadb_delete_entry(kcontext, lentry);
    } else {
        kerr = ipadb_delete_alias(kcontext, lentry, principal);
    }

done:
    ldap_msgfree(res);
    free(canonicalized);
    krb5_free_unparsed_name(kcontext, principal);
    return kerr;
}

krb5_error_code ipadb_iterate(krb5_context kcontext,
                              char *match_entry,
                              int (*func)(krb5_pointer, krb5_db_entry *),
                              krb5_pointer func_arg)
{
    struct ipadb_context *ipactx;
    krb5_error_code kerr;
    LDAPMessage *res = NULL;
    LDAPMessage *lentry;
    krb5_db_entry *kentry;
    uint32_t pol;

    ipactx = ipadb_get_context(kcontext);
    if (!ipactx) {
        return KRB5_KDB_DBNOTINITED;
    }

    /* If no match_entry is given iterate through all krb princs like the db2
     * or ldap plugin */
    if (match_entry == NULL) {
        match_entry = "*";
    }

    /* fetch list of principal matching filter */
    kerr = ipadb_fetch_principals(ipactx, 0, match_entry, &res);
    if (kerr != 0) {
        goto done;
    }

    lentry = ldap_first_entry(ipactx->lcontext, res);

    while (lentry) {

        kentry = NULL;
        kerr = ipadb_parse_ldap_entry(kcontext, NULL, lentry, &kentry, &pol);
        if (kerr == 0 && pol != 0) {
            kerr = ipadb_fetch_tktpolicy(kcontext, lentry, kentry, pol);
        }
        if (kerr == 0) {
            /* Now call the callback with the entry */
            func(func_arg, kentry);
        }
        ipadb_free_principal(kcontext, kentry);

        lentry = ldap_next_entry(ipactx->lcontext, lentry);
    }

    kerr = 0;

done:
    ldap_msgfree(res);
    return kerr;
}

