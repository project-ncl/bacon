#! /usr/bin/env python
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
import logging
import requests
import sys
import tempfile
import xml.etree.ElementTree as ET

logger = logging.getLogger(__name__)
logging.root.setLevel(logging.DEBUG)
logger.setLevel(logging.DEBUG)


MAVEN_CENTRAL_LINK = "https://repo.maven.org/maven2/org/jboss/pnc/ear-package/"
MAVEN_SNAPSHOT_LINK = "https://repository.jboss.org/nexus/content/repositories/snapshots/org/jboss/pnc/bacon/cli/"

USER_BACON_JAR_FOLDER_LOCATION = os.getenv("HOME") + "/.pnc-bacon/bin"
ROOT_BACON_JAR_FOLDER_LOCATION = "/opt/bacon/bin"

USER_SHELL_LOCATION = os.getenv("HOME") + "/bin"
ROOT_SHELL_LOCATION = "/usr/local/bin"

TEMPLATE_BASH = """
#!/bin/bash
set -e

if [ "$1" == "update" ]; then
    if [ "$2" == "snapshot" ]; then
        echo "Updating to latest snapshot version..."
    else
        echo "Updating to latest released version..."
    fi

    # Script runs the bacon_install.py to update itself
    curl -fsSL https://raw.github.com/project-ncl/bacon/master/bacon_install.py | python - $2
else
    java -jar {JAR_LOCATION}/bacon.jar "$@"
fi

if [ -z "$1" ]; then
    echo "To update to the latest released version of bacon, run:"
    echo ""
    echo "    bacon update"
    echo ""
    echo "To update to a snapshot version of bacon, run:"
    echo ""
    echo "    bacon update snapshot"
    echo ""
fi
"""

class BaconInstall:
    """
    Object responsible with installing bacon
    """
    def __init__(self, bacon_jar_location, shell_location, maven_url, snapshot = False):

        self.bacon_jar_location = bacon_jar_location
        self.shell_location = shell_location
        self.maven_url = maven_url
        self.latest_version = None
        self.snapshot = snapshot

    def run(self):
        """
        Install bacon jar

        Returns: None
        """
        latest_version = self.__get_latest_version()

        self.__create_folder_if_absent(self.bacon_jar_location)
        self.__create_folder_if_absent(self.shell_location)

        self.__download_latest_version()
        self.__create_bacon_shell_script()

        print("")
        logger.warning("Installed version: {}!".format(latest_version))

    def __download_latest_version(self):
        """
        Read the maven-metadata.xml of bacon and download the latest version
        """
        latest_version = self.__get_latest_version()

        if self.snapshot:
            snapshot_version = self.__get_latest_snapshot_version()
            download_link = self.maven_url + \
                latest_version + "/cli-" + snapshot_version + ".jar"
        else:
            download_link = self.maven_url + \
                latest_version + "/cli-" + latest_version + ".jar"

        self.__download_link(
            download_link,
            self.bacon_jar_location,
            "bacon.jar")

        logger.warning("bacon installed in: {}".format(
            self.bacon_jar_location + "/bacon.jar"))

    def __get_latest_version(self):
        """
        Read the maven-metadata.xml of bacon and parse the last released
        version
        """

        if self.latest_version is None:
            temp_folder = tempfile.mkdtemp()
            self.__download_maven_metadata_xml(self.maven_url, temp_folder)
            root = ET.parse(temp_folder + "/maven-metadata.xml").getroot()

            if self.snapshot:
                latest_tag = root.find("versioning/versions/version")
            else:
                latest_tag = root.find("versioning/latest")

            self.latest_version = latest_tag.text

        return self.latest_version

    def __get_latest_snapshot_version(self):

        latest_version = self.__get_latest_version()

        url = self.maven_url + latest_version
        temp_folder = tempfile.mkdtemp()
        self.__download_maven_metadata_xml(url, temp_folder)

        root = ET.parse(temp_folder + "/maven-metadata.xml").getroot()

        latest_tag = root.findall("versioning/snapshotVersions/snapshotVersion")
        return latest_tag[0].find("value").text

    def __download_maven_metadata_xml(self, url, folder):
        """
        Download the maven-metadata.xml for bacon and put it in folder

        url must be without maven-metadata.xml
        """
        if not url.endswith("/"):
            url = url + "/"

        download_link = url + "maven-metadata.xml"
        self.__download_link(download_link, folder, "maven-metadata.xml")

    def __download_link(self, link, folder, filename):

        logger.warning("Downloading: " + link)
        try:
            r = requests.get(link)

            logger.debug("Writing to: " + folder + "/" + filename)

            with open(folder + "/" + filename, "wb") as f:
                f.write(r.content)
        except Exception:
            raise Exception("Something wrong happened while downloading the link: " + link)

    def __create_folder_if_absent(self, folder):
        if not os.path.exists(folder):
            os.makedirs(folder)

    def __create_bacon_shell_script(self):

        filename = self.shell_location + "/bacon"

        with open(filename, "w") as f:
            f.write(TEMPLATE_BASH.format(JAR_LOCATION=self.bacon_jar_location))

        os.chmod(filename, 0o755)


def is_root():
    return os.geteuid() == 0


def main():
    """
    Main entry point to the program
    """
    snapshot = False
    if len(sys.argv) >= 2 and sys.argv[1] == 'snapshot':
        maven_link = MAVEN_SNAPSHOT_LINK
        snapshot = True
    else:
        maven_link = MAVEN_CENTRAL_LINK

    bacon_jar_location = USER_BACON_JAR_FOLDER_LOCATION
    shell_location = USER_SHELL_LOCATION

    if is_root():
        bacon_jar_location = ROOT_BACON_JAR_FOLDER_LOCATION
        shell_location = ROOT_SHELL_LOCATION

    bacon_install = BaconInstall(
            bacon_jar_location,
            shell_location,
            maven_link,
            snapshot=snapshot)
    try:
        bacon_install.run()
    except Exception as e:
        print(e)


if __name__ == "__main__":
    main()
