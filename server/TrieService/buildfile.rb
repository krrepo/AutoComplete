# Buildr 1.4.6 build file for the Search Platform builds
# GLG 2013

# Version number for this release
VERSION_NUMBER = "1.0.0"
GROUP = "Search & Recommendations"
COPYRIGHT = "(c) 2012-#{Time.now.year} - Gerson Lehrman Group"

# Specify Maven 2.0 remote repositories here, like this:
repositories.remote << "http://repo1.maven.org/maven2" << "http://www.ibiblio.org/maven2" 

CCOLLECTIONS = 'commons-collections:commons-collections:jar:3.2.1'
GRIZZLYFW = 'org.glassfish.grizzly:grizzly-framework:jar:2.2.21'
JCORE = 'org.codehaus.jackson:jackson-core-asl:jar:1.9.2'
JAVAX = 'org.glassfish:javax.servlet:jar:3.1'
JAXBAPI224 = 'javax.xml.bind:jaxb-api:jar:2.2.4'
JAXBIMPL224 = 'com.sun.xml.bind:jaxb-impl:jar:2.2.4-1'
JERSEYCORE = 'com.sun.jersey:jersey-core:jar:1.17.1'
JERSEYGRIZZLY = 'com.sun.jersey:jersey-grizzly2:jar:1.17.1'
JERSEYJSON = 'com.sun.jersey:jersey-json:jar:1.17.1'
JERSEYMULTIPART = 'com.sun.jersey.contribs:jersey-multipart:jar:1.17.1'
JERSEYSERVER = 'com.sun.jersey:jersey-server:jar:1.17.1'
JERSEYSPRING = 'com.sun.jersey.contribs:jersey-spring:jar:1.17.1'
JETTISON = 'org.codehaus.jettison:jettison:jar:1.1'
JSR = 'javax.ws.rs:jsr311-api:jar:1.1.1'
LBACK_CLASSIC = 'ch.qos.logback:logback-classic:jar:1.0.1'
OAUTHSRVR = 'com.sun.jersey.contribs.jersey-oauth:oauth-server:jar:1.17.1'
OPENCSV = 'net.sf.opencsv:opencsv:jar:2.3'
PERSISTENCE = 'javax.persistence:persistence-api:jar:1.0.2'
SLF4J = 'org.slf4j:slf4j-api:jar:1.6.4'
SOCKETIO = 'com.corundumstudio.socketio:netty-socketio:jar:1.0.1'

JACKSONDATABIND = 'com.fasterxml.jackson.core:jackson-databind:jar:2.2.2'
NETTY = 'io.netty:netty:jar:3.6.6.Final'

desc "The TrieService project"
define "TrieService" do

  project.version = VERSION_NUMBER
  project.group = GROUP
  manifest["Implementation-Vendor"] = COPYRIGHT
  manifest["Timestamp"] = Time.now

  GITHASH =  `git rev-parse HEAD`
  # The line above returns a carriage return at the end, and that causes a manifest file format error.
  # So, trim off the CR (hash is always 40 characters).
  manifest["Git-Hash"] = GITHASH[0..39]

  compile.with transitive(CCOLLECTIONS, GRIZZLYFW, JCORE, JAVAX, JAXBAPI224 , JAXBIMPL224, JERSEYCORE, JERSEYGRIZZLY, JERSEYJSON, JERSEYMULTIPART, JERSEYSERVER, JERSEYSPRING, JETTISON, JSR, LBACK_CLASSIC, OAUTHSRVR, OPENCSV, PERSISTENCE, SLF4J, NETTY, JACKSONDATABIND, SOCKETIO)

  compile.from './src'

  # Create the jar
  package :jar, :file=>_('trieservice.jar')
    
  packages.first.enhance do |task|
    task.enhance do
      compile.dependencies.map { |dep| FileUtils.cp dep.to_s , 'lib' }
    end
  end
end
