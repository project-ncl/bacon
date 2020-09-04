#! /usr/bin/env python
#
# JBoss, Home of Professional Open Source.
# Copyright 2017 Red Hat, Inc., and individual contributors
# as indicated by the @author tags.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

"""
This script must be compatible with Python 2 and Python 3

It downloads the bacon jar into BACON_JAR_FOLDER_LOCATION and sets up a Bash
script in ~/bin/bacon

The user is able to choose between the latest released jar from Maven Central
or the latest snapshot jar.

For the latter, run the script with argument 'snapshot':

    $ python bacon_install.py snapshot
or
    $ cat bacon_install.py | python - snapshot

If run without any argument, the latest released jar is downloaded by default


The Bash script wraps around the bacon jar so that the user can just type:

    $ bacon <command>

from the shell to run bacon commands.

The Bash script also supports update:

    $ bacon update
    $ bacon update snapshot

'bacon update' just re-downloads this script from Github to download the
relevant bacon jar and the new bacon Bash script
"""
import os
import platform
import sys
import tempfile
import xml.etree.ElementTree as ET


MAVEN_CENTRAL_LINK = "https://repo1.maven.org/maven2/org/jboss/pnc/bacon/cli/"
MAVEN_SNAPSHOT_LINK = "https://repository.jboss.org/nexus/content/repositories/snapshots/org/jboss/pnc/bacon/cli/"

USER_BACON_JAR_FOLDER_LOCATION = os.getenv("HOME") + "/.pnc-bacon/bin"
ROOT_BACON_JAR_FOLDER_LOCATION = "/opt/bacon/bin"

USER_SHELL_FOLDER_LOCATION = os.getenv("HOME") + "/bin"
ROOT_SHELL_FOLDER_LOCATION = "/usr/local/bin"

TEMPLATE_BASH = """
#!/bin/bash
set -e

function check_if_java_installed {{
    command -v java > /dev/null 2>&1 || {{ echo >&2 "java is required to run this command... Aborting!"; exit 1; }}
}}

if [ "$1" == "update" ]; then
    if [ "$2" == "snapshot" ]; then
        echo "Updating to latest snapshot version..."
    elif [ -z "$2" ]; then
        echo "Updating to latest released version..."
    else
        echo "Updating to version $2"
    fi

    # Script runs the bacon_install.py to update itself
    curl -fsSL https://raw.github.com/project-ncl/bacon/master/bacon_install.py | python3 - $2
else
    check_if_java_installed

    if [ "$OSTYPE" = "cygwin" ]; then
        java -jar `cygpath -w {0}/bacon.jar` {1} "$@"
    else
        java -jar {0}/bacon.jar {1} "$@"
    fi
fi

if [ -z "$1" ]; then
    echo "To update to the latest released version of bacon/pnc/da/pig, run:"
    echo ""
    echo "    bacon update"
    echo ""
    echo "To update to a specific released version of bacon/pnc/da/pig, run:"
    echo ""
    echo "    bacon update <version>"
    echo ""
    echo "To update to a snapshot version of bacon, run:"
    echo ""
    echo "    bacon update snapshot"
    echo ""
fi
""".strip()


def print_mac_notice_if_required():
    """
    Print this notice for Mac users since their PATH env var is different than
    in Linux
    """

    if platform.system() == 'Darwin':
        print("")
        print("Mac user detected! Please adjust your $PATH variable if" +
              " necessary to run 'bacon'")
        print("")
        print("    $ echo 'export PATH=\"$PATH:${HOME}/bin\"' >> " +
              "~/.bash_profile")


def download_maven_metadata_xml(url, folder):
    """
    Download the maven-metadata.xml for bacon and put it in folder

    url must be without maven-metadata.xml
    """
    if not url.endswith("/"):
        url = url + "/"

    link = url + "maven-metadata.xml"
    download_link(link, folder, "maven-metadata.xml")


def download_link(link, folder, filename):
    """
    Download the link into the folder with name 'filename'
    """

    print("Downloading: " + link)
    if sys.version_info[0] >= 3:
        import urllib.request as request
    else:
        import urllib2 as request

    try:
        r = request.urlopen(link).read()

        with open(folder + "/" + filename, "wb") as f:
            f.write(r)
    except Exception:
        raise Exception("Something wrong happened while downloading the link: " + link)


def create_folder_if_absent(folder):
    if not os.path.exists(folder):
        os.makedirs(folder)

class BaconInstall:
    """
    Object responsible with installing bacon
    """
    def __init__(self, bacon_jar_location, shell_location, maven_url, version=None):

        self.bacon_jar_location = bacon_jar_location
        self.shell_location = shell_location
        self.maven_url = maven_url
        self.latest_version = None
        self.version = version

    def __is_snapshot(self):
        if self.version and self.version == "snapshot":
            return True

        return False

    def run(self):
        """
        Install bacon jar

        Returns: None
        """
        create_folder_if_absent(self.bacon_jar_location)
        create_folder_if_absent(self.shell_location)

        self.latest_version = self.__get_latest_version()

        self.__download_version()
        self.__create_bacon_shell_script()

        print("")
        print("Installed version: {}!".format(self.latest_version))
        print_mac_notice_if_required()

    def __download_version(self):
        """
        Read the maven-metadata.xml of bacon and download the latest version
        """

        if self.version:
            if self.__is_snapshot():
                snapshot_version = self.__get_latest_snapshot_version()
                url = self.maven_url + \
                    self.latest_version + "/cli-" + snapshot_version + "-shaded.jar"
            else:
                url = self.maven_url + \
                    self.version + "/cli-" + self.version + "-shaded.jar"
        else:
            url = self.maven_url + \
                self.latest_version + "/cli-" + self.latest_version + "-shaded.jar"

        download_link(url, self.bacon_jar_location, "bacon.jar")

        print("bacon installed in: {}".format(
            self.bacon_jar_location + "/bacon.jar"))

    def __get_latest_version(self):
        """
        Read the maven-metadata.xml of bacon and parse the last released
        version
        """

        temp_folder = tempfile.mkdtemp()
        download_maven_metadata_xml(self.maven_url, temp_folder)
        root = ET.parse(temp_folder + "/maven-metadata.xml").getroot()

        if self.__is_snapshot():
            latest_tags = root.findall("versioning/versions/version")

            # choose the one listed last. This might bite us in the future
            latest_tag = latest_tags[-1]
        else:
            latest_tag = root.find("versioning/latest")

        return latest_tag.text

    def __get_latest_snapshot_version(self):

        url = self.maven_url + self.latest_version
        temp_folder = tempfile.mkdtemp()
        download_maven_metadata_xml(url, temp_folder)

        root = ET.parse(temp_folder + "/maven-metadata.xml").getroot()

        latest_tag = root.findall("versioning/snapshotVersions/snapshotVersion")
        return latest_tag[0].find("value").text

    def __create_bacon_shell_script(self):

        filename_bacon = self.shell_location + "/bacon"
        filename_pnc = self.shell_location + "/pnc"
        filename_da = self.shell_location + "/da"
        filename_pig = self.shell_location + "/pig"

        with open(filename_bacon, "w") as f:
            f.write(TEMPLATE_BASH.format(self.bacon_jar_location, ""))
        with open(filename_pnc, "w") as f:
            f.write(TEMPLATE_BASH.format(self.bacon_jar_location, "pnc"))
        with open(filename_da, "w") as f:
            f.write(TEMPLATE_BASH.format(self.bacon_jar_location, "da"))
        with open(filename_pig, "w") as f:
            f.write(TEMPLATE_BASH.format(self.bacon_jar_location, "pig"))

        os.chmod(filename_bacon, 0o755)
        os.chmod(filename_pnc, 0o755)
        os.chmod(filename_da, 0o755)
        os.chmod(filename_pig, 0o755)


def is_root():
    return os.geteuid() == 0


def main():
    """
    Main entry point to the program
    """
    version = None
    maven_link = MAVEN_CENTRAL_LINK

    if len(sys.argv) >= 2:
        version = sys.argv[1]

        if sys.argv[1] == 'snapshot':
            maven_link = MAVEN_SNAPSHOT_LINK

    bacon_jar_location = USER_BACON_JAR_FOLDER_LOCATION
    shell_location = USER_SHELL_FOLDER_LOCATION

    if is_root():
        bacon_jar_location = ROOT_BACON_JAR_FOLDER_LOCATION
        shell_location = ROOT_SHELL_FOLDER_LOCATION

    bacon_install = BaconInstall(
            bacon_jar_location,
            shell_location,
            maven_link,
            version=version)
    try:
        bacon_install.run()
    except Exception as e:
        print(e)
        sys.exit(1)


if __name__ == "__main__":
    main()
