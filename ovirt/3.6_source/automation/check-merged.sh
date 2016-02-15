#!/bin/bash -xe

BUILD_UT=0
SUFFIX=".git$(git rev-parse --short HEAD)"
MAVEN_SETTINGS="/etc/maven/settings.xml"
export BUILD_JAVA_OPTS_MAVEN="\
    -XX:MaxPermSize=1G \
    -Dgwt.compiler.localWorkers=2 \
"
export EXTRA_BUILD_FLAGS="-gs $MAVEN_SETTINGS"
export BUILD_JAVA_OPTS_GWT="\
    -XX:PermSize=512M \
    -XX:MaxPermSize=1G \
    -Xms1G \
    -Xmx6G \
"

# Use ovirt mirror if able, fall back to central maven
mkdir -p "${MAVEN_SETTINGS%/*}"
cat >"$MAVEN_SETTINGS" <<EOS
<?xml version="1.0"?>
<settings xmlns="http://maven.apache.org/POM/4.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
          http://maven.apache.org/xsd/settings-1.0.0.xsd">

<mirrors>
        <mirror>
                <id>ovirt-maven-repository</id>
                <name>oVirt artifactory proxy</name>
                <url>http://artifactory.ovirt.org:8081/artifactory/ovirt-mirror</url>
                <mirrorOf>*</mirrorOf>
        </mirror>
        <mirror>
                <id>root-maven-repository</id>
                <name>Official maven repo</name>
                <url>http://repo.maven.apache.org/maven2</url>
                <mirrorOf>*</mirrorOf>
        </mirror>
</mirrors>
</settings>
EOS
# remove any previous artifacts
rm -rf output
rm -f ./*tar.gz
make clean \
    "EXTRA_BUILD_FLAGS=$EXTRA_BUILD_FLAGS"

# Get the tarball
make dist

# create the src.rpm
rpmbuild \
    -D "_srcrpmdir $PWD/output" \
    -D "_topmdir $PWD/rpmbuild" \
    -D "release_suffix ${SUFFIX}" \
    -D "ovirt_build_extra_flags $EXTRA_BUILD_FLAGS" \
    -D "ovirt_build_quick 1" \
    -ts ./*.gz

# install any build requirements
yum-builddep output/*src.rpm

if [[ "$(rpm --eval "%{dist}")" ==  ".fc22" ]]; then
    # Needed to download the deps before compilation, for some reason it fails
    # inside fc22 chroot if not done first
    mvn dependency:resolve-plugins \
        $EXTRA_BUILD_FLAGS
fi

# create the rpms
rpmbuild \
    -D "_rpmdir $PWD/output" \
    -D "_topmdir $PWD/rpmbuild" \
    -D "release_suffix ${SUFFIX}" \
    -D "ovirt_build_ut $BUILD_UT" \
    -D "ovirt_build_extra_flags $EXTRA_BUILD_FLAGS" \
    -D "ovirt_build_quick 1" \
    --rebuild output/*.src.rpm

# Store any relevant artifacts in exported-artifacts for the ci system to
# archive
[[ -d exported-artifacts ]] || mkdir -p exported-artifacts
find output -iname \*rpm -exec mv "{}" exported-artifacts/ \;
mv ./*tar.gz exported-artifacts/
