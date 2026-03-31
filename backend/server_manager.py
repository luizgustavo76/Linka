import os
import shutil
import plataform
import subprocess
system_os = platform.system()


if shutil.which("git"):
    pass
else:
    print("Git is not installed, want install now?[Y/N].")
    if system_os == "windows":
        subprocess.run(["winget", "install", "git.git"])
    