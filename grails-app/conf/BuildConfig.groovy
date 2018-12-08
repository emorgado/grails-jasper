grails.project.work.dir = 'target'

grails.project.dependency.resolver = 'maven'

// grails.project.repos.grailsCentral.username = System.getenv("GRAILS_CENTRAL_USERNAME")
// grails.project.repos.grailsCentral.password = System.getenv("GRAILS_CENTRAL_PASSWORD")

grails.project.repos.velumsoft.url = "http://velumsoft.com.br:8080/archiva/repository/public"
grails.project.repos.velumsoft.type = "maven"
grails.project.repos.velumsoft.username = System.getenv("ARCHIVA_USERNAME")
grails.project.repos.velumsoft.password = System.getenv("ARCHIVA_USERNAME")

grails.project.dependency.resolution = {

    inherits 'global'
    log 'warn'

    repositories {
        grailsCentral()
        mavenLocal()
        mavenCentral()
        mavenRepo "http://jaspersoft.artifactoryonline.com/jaspersoft/third-party-ce-artifacts/"
    }

    dependencies {
        compile('com.lowagie:itext:2.1.7')

        compile 'net.sf.jasperreports:jasperreports-fonts:6.0.0'
        compile('net.sf.jasperreports:jasperreports:6.7.0') {
            excludes 'antlr', 'commons-logging',
                     'ant', 'mondrian', 'commons-javaflow','barbecue', 'xml-apis-ext','xml-apis', 'xalan', 'groovy-all', 'hibernate', 'saaj-api', 'servlet-api',
                     'xercesImpl','xmlParserAPIs','spring-core','bsh', 'spring-beans', 'jaxen', 'barcode4j','batik-svg-dom','batik-xml','batik-awt-util','batik-dom',
                     'batik-css','batik-gvt','batik-script', 'batik-svggen','batik-util','batik-bridge','persistence-api','jdtcore','bcmail-jdk16','bcprov-jdk16','bctsp-jdk16',
                     'bcmail-jdk14','bcprov-jdk14','bctsp-jdk14','xmlbeans', 'olap4j'
        }

        compile('org.apache.poi:poi:3.17')
    }

    plugins {
        build ':release:3.0.1', ':rest-client-builder:2.0.3', {
            export = false
        }

        test(":hibernate4:4.3.6.1") {
            export = false
        }
        test(":tomcat:8.0.20"){
            export = false
        }
    }


}
