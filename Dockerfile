#Dockerfile

FROM phusion/baseimage:0.9.17

MAINTAINER Greg Leitheiser <greg@servantscode.org>

RUN echo "deb http://archive.ubuntu.com/ubuntu trusty main universe" > /etc/apt/sources.list

RUN apt-get -y update

# Install Java
ENV JAVA_VER 10
ENV JAVA_HOME /usr/lib/jvm/java-10-oracle

RUN echo 'deb http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main' >> /etc/apt/sources.list && \
    echo 'deb-src http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main' >> /etc/apt/sources.list && \
    apt-key adv --keyserver keyserver.ubuntu.com --recv-keys C2518248EEA14886 && \
    sudo add-apt-repository ppa:linuxuprising/java && \
    apt-get update && \
    echo oracle-java${JAVA_VER}-installer shared/accepted-oracle-license-v1-1 select true | sudo /usr/bin/debconf-set-selections && \
    apt-get install -y --force-yes --no-install-recommends oracle-java${JAVA_VER}-installer oracle-java${JAVA_VER}-set-default && \
    apt-get clean && \
    rm -rf /var/cache/oracle-jdk${JAVA_VER}-installer

# Set Oracle as default
RUN update-java-alternatives -s java-10-oracle

RUN echo "export JAVA_HOME=/usr/lib/jvm/java-10-oracle" >> ~/.bashrc

#Clean APT
RUN apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

COPY classes/artifacts/fake_data_jar/* /usr/local/fake-data/lib/
copy scripts/* /usr/local/fake-data/bin/

#Init
CMD ["/usr/local/fake-data/bin/exec.sh"]

