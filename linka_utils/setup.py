from setuptools import setup, find_packages

setup(
    name="linka",
    version="0.1.0",
    packages=find_packages(),
    install_requires=[
        "flask",
        "requests"
    ],
    author="Luiz Gustavo Custódio Ferreira Lourenço",
    description="linka federations api",
    long_description=open("README.md").read(),
    long_description_content_type="text/markdown",
    url="https://github.com/luizgustavo76/Linka",
    classifiers=[
        "Programming Language :: Python :: 3",
        "Framework :: Flask",
        "License :: OSI Approved :: MIT License"
    ],
    python_requires=">=3.8",
)