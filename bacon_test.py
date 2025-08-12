import bacon_install

import os
import shutil
import subprocess
import tempfile
import unittest


class TestBacon(unittest.TestCase):

    def test_create_folder_if_absent(self):
        with tempfile.TemporaryDirectory() as f:
            # make sure no exceptions thrown
            bacon_install.create_folder_if_absent(f)

        # create a temp_folder and delete it
        temp_folder = tempfile.mkdtemp()
        shutil.rmtree(temp_folder)

        # see if create_folder_if_absent creates it back
        bacon_install.create_folder_if_absent(temp_folder)
        self.assertTrue(os.path.exists(temp_folder))


    def test_download_link(self):
        with tempfile.TemporaryDirectory() as f:
            bacon_install.download_link(
                    "https://repo1.maven.org/maven2/org/jboss/pnc/bacon/cli/maven-metadata.xml",
                    f, "testme")
            self.assertTrue(os.path.exists(os.path.join(f, "testme")))

    def test_calculate_sha1(self):
        f = tempfile.NamedTemporaryFile(delete=False)
        f.write(b"hello\n")
        f.close()

        self.assertEqual(
                bacon_install.calculate_sha1(f.name),
                "f572d396fae9206628714fb2ce00f72e94f2258f")

        # cleanup
        os.remove(f.name)

    def test_bacon_install(self):
        bacon_jar_location = tempfile.mkdtemp()
        shell_location = tempfile.mkdtemp()
        maven_link = "https://repo1.maven.org/maven2/org/jboss/pnc/bacon/cli/"
        autocomplete_link = "https://github.com/project-ncl/bacon/raw/refs/heads/main/bacon_completion"

        install = bacon_install.BaconInstall(
                bacon_jar_location,
                shell_location,
                maven_link,
                autocomplete_link)
        install.run()

        self.assertTrue(os.path.exists(os.path.join(bacon_jar_location, "bacon.jar")))
        self.assertTrue(os.path.exists(os.path.join(shell_location, "bacon")))
        self.assertTrue(os.path.exists(os.path.join(shell_location, "da")))
        self.assertTrue(os.path.exists(os.path.join(shell_location, "pnc")))
        self.assertTrue(os.path.exists(os.path.join(shell_location, "pig")))

        # test if bacon.jar runs
        bacon_version = subprocess.run([
            "java",
            "-jar",
            os.path.join(bacon_jar_location, "bacon.jar"),
            "--version"],
            stdout=subprocess.PIPE)

        self.assertIn("Bacon version", str(bacon_version.stdout))
        self.assertEqual(bacon_version.returncode, 0)

        # cleanup
        shutil.rmtree(bacon_jar_location)
        shutil.rmtree(shell_location)


    def test_bacon_install_with_version(self):
        bacon_jar_location = tempfile.mkdtemp()
        shell_location = tempfile.mkdtemp()
        maven_link = "https://repo1.maven.org/maven2/org/jboss/pnc/bacon/cli/"
        autocomplete_link = "https://github.com/project-ncl/bacon/raw/refs/tags/3.2.1/bacon_completion"


        install = bacon_install.BaconInstall(
                bacon_jar_location,
                shell_location,
                maven_link,
                autocomplete_link,
                version="2.1.8")
        install.run()

        self.assertTrue(os.path.exists(os.path.join(bacon_jar_location, "bacon.jar")))
        self.assertTrue(os.path.exists(os.path.join(shell_location, "bacon")))
        self.assertTrue(os.path.exists(os.path.join(shell_location, "da")))
        self.assertTrue(os.path.exists(os.path.join(shell_location, "pnc")))
        self.assertTrue(os.path.exists(os.path.join(shell_location, "pig")))

        bacon_version = subprocess.run([
            "java",
            "-jar",
            os.path.join(bacon_jar_location, "bacon.jar"),
            "--version"],
            stdout=subprocess.PIPE)

        self.assertTrue("2.1.8", str(bacon_version.stdout))
        self.assertEqual(bacon_version.returncode, 0)

        # cleanup
        shutil.rmtree(bacon_jar_location)
        shutil.rmtree(shell_location)
