CREATE FUNCTION __temp_set_display_migration() RETURNS VOID AS $$
DECLARE
    mgmt_name CHARACTER VARYING(15);
BEGIN
    SELECT option_value
    FROM vdc_options
    WHERE option_name='ManagementNetwork'
    INTO mgmt_name;

    UPDATE network_cluster nc1
    SET is_display = true
    WHERE EXISTS (SELECT 1
                  FROM network
                  WHERE network.id = nc1.network_id AND name = mgmt_name)
        AND NOT EXISTS (SELECT 1
                        FROM network_cluster nc2
                        WHERE nc2.cluster_id = nc1.cluster_id AND nc2.is_display);

    UPDATE network_cluster nc1
    SET migration = true
    WHERE EXISTS (SELECT 1
                  FROM network
                  WHERE network.id = nc1.network_id AND name = mgmt_name)
        AND NOT EXISTS (SELECT 1
                        FROM network_cluster nc2
                        WHERE nc2.cluster_id = nc1.cluster_id AND nc2.migration);
END;
$$ LANGUAGE plpgsql;

SELECT __temp_set_display_migration();

DROP FUNCTION __temp_set_display_migration();
