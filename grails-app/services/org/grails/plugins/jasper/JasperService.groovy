/* Copyright 2006-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.grails.plugins.jasper

 import groovy.sql.Sql

import java.lang.reflect.Field
import java.sql.Connection

import net.sf.jasperreports.engine.JRAbstractExporter
import net.sf.jasperreports.engine.JRDataSource
import net.sf.jasperreports.engine.JRException
import net.sf.jasperreports.engine.JasperCompileManager
import net.sf.jasperreports.engine.JasperFillManager
import net.sf.jasperreports.engine.JasperPrint
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource
import net.sf.jasperreports.engine.export.HtmlExporter
import net.sf.jasperreports.engine.export.JRCsvExporter
import net.sf.jasperreports.engine.export.JRExporterContext
import net.sf.jasperreports.engine.export.JRPdfExporter
import net.sf.jasperreports.engine.export.JRRtfExporter
import net.sf.jasperreports.engine.export.JRTextExporter
import net.sf.jasperreports.engine.export.JRXlsExporter
import net.sf.jasperreports.engine.export.JRXmlExporter
import net.sf.jasperreports.engine.export.JsonExporter
import net.sf.jasperreports.engine.export.oasis.JROdsExporter
import net.sf.jasperreports.engine.export.oasis.JROdtExporter
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter
import net.sf.jasperreports.engine.export.ooxml.JRPptxExporter
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter
import net.sf.jasperreports.export.ExporterConfiguration
import net.sf.jasperreports.export.ExporterOutput
import net.sf.jasperreports.export.ReportExportConfiguration
import net.sf.jasperreports.export.SimpleExporterInput
import net.sf.jasperreports.export.SimpleHtmlExporterOutput
import net.sf.jasperreports.export.SimpleHtmlReportConfiguration
import net.sf.jasperreports.export.SimpleJsonExporterConfiguration
import net.sf.jasperreports.export.SimpleJsonExporterOutput
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput
import net.sf.jasperreports.export.SimpleTextExporterConfiguration
import net.sf.jasperreports.export.SimpleWriterExporterOutput
import net.sf.jasperreports.export.SimpleXlsReportConfiguration
import net.sf.jasperreports.export.SimpleXmlExporterOutput

import org.grails.plugins.jasper.JasperExportFormat;
import org.springframework.core.io.Resource
import org.springframework.transaction.annotation.Transactional


/**
 * Generates Jasper reports. Call one of the three generateReport methods to
 * get a ByteArrayOutputStream with the generated report.
 * @author Sebastian Hohns
 */
@Transactional(readOnly = true)
class JasperService {

    def dataSource

    static final boolean FORCE_TEMP_FOLDER = false

    /**
     * Build a JasperReportDef form a parameter map. This is used by the taglib.
     * @param parameters
     * @param locale
     * @param testModel
     * @return reportDef
     */
    JasperReportDef buildReportDefinition(Map<String, Object>parameters, locale, testModel) {
        log.debug("buildReportDefinition")
        JasperReportDef reportDef = new JasperReportDef(name: parameters._file, parameters: parameters,locale: locale)

        reportDef.fileFormat = JasperExportFormat.determineFileFormat(parameters._format)
        reportDef.reportData = getReportData(testModel, parameters)
        reportDef.contentStream = generateReport(reportDef)
        reportDef.jasperPrinter = generatePrinter(reportDef)

        return reportDef
    }

    private Collection getReportData(testModel, Map<String, Object> parameters) {
        Collection reportData

        if (testModel?.data) {
            try {
                reportData = testModel.data
            } catch (Throwable e) {
                throw new Exception("Expected chainModel.data parameter to be a Collection, but it was ${chainModel.data.class.name}", e)
            }
        } else {
            testModel = getProperties().containsKey('model') ? model : null
            if (testModel?.data) {
                try {
                    reportData = testModel.data
                } catch (Throwable e) {
                    throw new Exception("Expected model.data parameter to be a Collection, but it was ${model.data.class.name}", e)
                }
            } else if (parameters?.data) {
                try {
                    reportData = parameters.data
                } catch (Throwable e) {
                    throw new Exception("Expected data parameter to be a Collection, but it was ${parameters.data.class.name}", e)
                }
            }
        }

        return reportData
    }

    @Deprecated
    ByteArrayOutputStream generateReport(String jasperReportDir, JasperExportFormat format, Collection reportData, Map parameters) {
        JasperReportDef reportDef = new JasperReportDef(name: parameters._file, folder: jasperReportDir, reportData: reportData, fileFormat: format, parameters: parameters)
        return generateReport(reportDef)
    }

    /**
     * Generate a report based on a single jasper file.
     * @param format , target format
     * @param reportDef , jasper report object
     * return ByteArrayOutStreamByteArrayOutStream with the generated Report
     */
    ByteArrayOutputStream generateReport(JasperReportDef reportDef) {
        log.info("generateReport " +reportDef)
        ByteArrayOutputStream outStream = new ByteArrayOutputStream(); 
        
                
        if ( reportDef.jasperPrinter == null ) {
            
            reportDef.jasperPrinter = generatePrinter(reportDef)
        }
        log.debug( "jasperprint is null? "+ ( reportDef.jasperPrinter == null ) )
                
        JRAbstractExporter exporter = getExporter( reportDef.fileFormat, outStream, reportDef.jasperPrinter );
        
        /*/
        JRAbstractExporter exporter = getExporter( reportDef.fileFormat );
        exporter.setExporterInput( new SimpleExporterInput( reportDef.jasperPrinter ) );        
        exporter.setExporterOutput( new SimpleOutputStreamExporterOutput( outStream ) );
        //*/
        exporter.exportReport();
        
        return outStream
    }
    
    private JRAbstractExporter getExporter( JasperExportFormat format, ByteArrayOutputStream outStream, JasperPrint jasperPrinter ){
        
        JRAbstractExporter exporter
        
        switch( format ){
            case JasperExportFormat.PDF_FORMAT:
                exporter = new JRPdfExporter();
                exporter.setExporterInput( new SimpleExporterInput( jasperPrinter ) );
                exporter.setExporterOutput( new SimpleOutputStreamExporterOutput( outStream ) );
                break; 
            case JasperExportFormat.HTML_FORMAT:
                exporter = new HtmlExporter();
                exporter.setExporterInput( new SimpleExporterInput( jasperPrinter ) );
                exporter.setExporterOutput( new SimpleHtmlExporterOutput( outStream ) );
                break;
            case JasperExportFormat.XML_FORMAT:
                exporter = new JRXmlExporter();
                exporter.setExporterInput( new SimpleExporterInput( jasperPrinter ) );
                exporter.setExporterOutput( new SimpleXmlExporterOutput( outStream ) );
                break;
            case JasperExportFormat.CSV_FORMAT:
                exporter = new JRCsvExporter();
                exporter.setExporterInput( new SimpleExporterInput( jasperPrinter ) );
                exporter.setExporterOutput( new SimpleWriterExporterOutput( outStream ) );
                break;
            case JasperExportFormat.XLS_FORMAT:
                exporter = new JRXlsExporter();
                exporter.setExporterInput( new SimpleExporterInput( jasperPrinter ) );
                exporter.setExporterOutput( new SimpleOutputStreamExporterOutput( outStream ) );
                SimpleXlsReportConfiguration configuration = new SimpleXlsReportConfiguration();
                configuration.setOnePagePerSheet(false);
                exporter.setConfiguration(configuration);
            break;
            case JasperExportFormat.RTF_FORMAT:
                exporter = new JRRtfExporter();
                exporter.setExporterInput( new SimpleExporterInput( jasperPrinter ) );
                exporter.setExporterOutput( new SimpleOutputStreamExporterOutput( outStream ) );
                break;
            case JasperExportFormat.TEXT_FORMAT:
                exporter = new JRTextExporter();
                exporter.setExporterInput( new SimpleExporterInput( jasperPrinter ) );
                exporter.setExporterOutput( new SimpleOutputStreamExporterOutput( outStream ) );
            break;
            case JasperExportFormat.ODT_FORMAT:
                exporter = new JROdtExporter();
                exporter.setExporterInput( new SimpleExporterInput( jasperPrinter ) );
                exporter.setExporterOutput( new SimpleOutputStreamExporterOutput( outStream ) );
                break;
            case JasperExportFormat.ODS_FORMAT:
                exporter = new JROdsExporter();
                exporter.setExporterInput( new SimpleExporterInput( jasperPrinter ) );
                exporter.setExporterOutput( new SimpleOutputStreamExporterOutput( outStream ) );
                break;
            case JasperExportFormat.DOCX_FORMAT:
                exporter = new JRDocxExporter();
                exporter.setExporterInput( new SimpleExporterInput( jasperPrinter ) );
                exporter.setExporterOutput( new SimpleOutputStreamExporterOutput( outStream ) );
                break;
            case JasperExportFormat.XLSX_FORMAT:
                exporter = new JRXlsxExporter();
                exporter.setExporterInput( new SimpleExporterInput( jasperPrinter ) );
                exporter.setExporterOutput( new SimpleOutputStreamExporterOutput( outStream ) );
                break;
            case JasperExportFormat.PPTX_FORMAT:
                exporter = new JRPptxExporter();
                exporter.setExporterInput( new SimpleExporterInput( jasperPrinter ) );
                exporter.setExporterOutput( new SimpleOutputStreamExporterOutput( outStream ) );
                break;
            case JasperExportFormat.JSON_FORMAT:
                exporter = new JsonExporter();
                exporter.setExporterInput( new SimpleExporterInput( jasperPrinter ) );
                exporter.setExporterOutput( new SimpleJsonExporterOutput( outStream ) );
                SimpleJsonExporterConfiguration configuration = new SimpleJsonExporterConfiguration();                
                exporter.setConfiguration( configuration );
                break;
            default:
                throw new JRException("Unknown report format: " + format );
        }
        
        //exporter.setExporterInput( new SimpleExporterInput( jasperPrinter ) );
        return exporter;
    }

    /**
     * Generate a single report based on a list of jasper files.
     * @param format , target format
     * @param reports , a List with report objects
     * @param parameters , additional parameters
     * return ByteArrayOutStream with the generated Report
     */
    ByteArrayOutputStream generateReport(List<JasperReportDef> reports) {
        log.info("generateReport from report list")
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        
        JRAbstractExporter exporter = getExporter( reports.first() );        
        
        List<JasperPrint> printers = reports.collect { report -> generatePrinter(report) }
        
        exporter.setExporterInput( new SimpleExporterInput( printers ) );
        exporter.setExporterOutput( new SimpleOutputStreamExporterOutput( outStream ) );
        exporter.exportReport();
        
        return outStream
    }

    /**
     * Forces the Jasper Reports  temp folder to be "~/.grails/.jasper" and ensures that such a folder exists.
     * The user (however the app server is logged in) is much more likely to have read/write/delete rights here than the
     * default location that Jasper Reports uses.
     */
    protected void forceTempFolder() {
        /* TODO This is currently disabled, because it doesn't work. Jasper Reports seems to always use the current
         * folder (.) no matter what.  (I'll be filing a bug report against Jasper Reports itself shortly - Craig Jones 16-Aug-2008)
         */
        if (FORCE_TEMP_FOLDER) {
            // Look up the home folder explicitly (don't trust that tilde notation will work).
            String userHomeDir = System.getProperty('user.home')
            File tempFolder = new File(userHomeDir, "/.grails/.jasper")

            // This is the current official means for setting the temp folder for jasper reports to use when compiling
            // reports on the fly, but it doesn't work
            JRProperties.setProperty(JRProperties.COMPILER_TEMP_DIR, tempFolder.getAbsolutePath())

            // This is a deprecated means for setting the temp folder that supposedly still works (still in the Jasper
            // Reports source code trunk as of 14-Aug-2008, and, in fact, takes precedence over the official method);
            // however, it doesn't work either.
            System.setProperty("jasper.reports.compile.temp", tempFolder.getAbsolutePath())

            if (!tempFolder.exists()) {
                def ant = new AntBuilder()
                ant.mkdir(dir: tempFolder.getAbsolutePath())
                if (!tempFolder.exists()) {
                    throw new Exception("Unable to create temp folder: ${tempFolder.getPath()}")
                }
            }
        }
    }

    /**
     * Generate a exporter with for a JasperReportDef. Note that SUBREPORT_DIR an locale have default
     * values.
     * @param reportDef
     * @return JRExporter
     */
    private JRAbstractExporter generateExporter(JasperReportDef reportDef) {
        if (reportDef.parameters.SUBREPORT_DIR == null) {
            reportDef.parameters.SUBREPORT_DIR = reportDef.getFilePath()
        }

        if (reportDef.parameters.locale) {
            if (reportDef.parameters.locale instanceof String) {
                reportDef.parameters.REPORT_LOCALE = getLocaleFromString(reportDef.parameters.locale)
            } else if (reportDef.parameters.locale instanceof Locale) {
                reportDef.parameters.REPORT_LOCALE = reportDef.parameters.locale
            }
        } else if (reportDef.locale) {
            reportDef.parameters.REPORT_LOCALE = reportDef.locale
        } else {
            reportDef.parameters.REPORT_LOCALE = Locale.getDefault()
        }

        JRAbstractExporter exporter = JasperExportFormat.getExporter(reportDef.fileFormat)
        Field[] fields = JasperExportFormat.getExporterFields(reportDef.fileFormat)

        Boolean useDefaultParameters = reportDef.parameters.useDefaultParameters.equals("true")
        if (useDefaultParameters) {
            applyDefaultParameters(exporter, reportDef.fileFormat)
        }

        if (fields) {
            applyCustomParameters(fields, exporter, reportDef.parameters)
        }

        return exporter
    }

    /**
     * Generate a JasperPrint object for a given report.
     * @param reportDefinition , the report
     * @param parameters , additional parameters
     * @return JasperPrint , jasperreport printer
     */
    private JasperPrint generatePrinter(JasperReportDef reportDef) {
        log.info("generatePrinter")
        log.debug( "      reportDef null? " + reportDef == null )
        log.debug( "    resource is null? " + reportDef?.getReport() == null )
        log.debug( "jrDataSource is null? " + reportDef?.dataSource == null )
        
        JasperPrint  jasperPrint
        Resource     resource = reportDef.getReport()
        JRDataSource jrDataSource = reportDef.dataSource

        
        if (jrDataSource == null && reportDef.reportData != null && !reportDef.reportData.isEmpty()) {
            log.debug( "no jrDataSource, reportData: "+( reportDef.reportData != null && !reportDef.reportData.isEmpty() ) )
            jrDataSource = new JRBeanCollectionDataSource(reportDef.reportData)
        }

        if (jrDataSource != null) {
            
            log.debug( "with jrDataSource" )
            
            if (resource.getFilename().endsWith('.jasper')) {
                log.debug( "is .jasper file" )
                jasperPrint = JasperFillManager.fillReport(resource.inputStream, reportDef.parameters, jrDataSource)
                
            } else {
                log.debug( "is not a .jasper file" )
                forceTempFolder()
                
                jasperPrint = JasperFillManager.fillReport(JasperCompileManager.compileReport(resource.inputStream), reportDef.parameters, jrDataSource)
            }
        } else {
            log.debug( "sql Datasource" )
            Sql sql = new Sql(dataSource)
            Connection connection = dataSource?.getConnection()

            try {
                if (resource.getFilename().endsWith('.jasper')) {
                    log.debug( "is .jasper file" )
                    jasperPrint = JasperFillManager.fillReport(resource.inputStream, reportDef.parameters, connection)
                }
                else {
                    log.debug( "is not a .jasper file" )
                    forceTempFolder()
                    jasperPrint = JasperFillManager.fillReport(JasperCompileManager.compileReport(resource.inputStream), reportDef.parameters,  connection)
                }
            }
            finally {
                sql.close()
                connection.close()
            }
        }
        log.debug("printer generated?: " + (jasperPrint != null))
        reportDef.jasperPrinter = jasperPrint
        return jasperPrint
    }

    /**
     * Apply additional parameters to the exporter. If the user submits a parameter that is not available for
     * the file format this parameter is ignored.
     * @param fields , available fields for the choosen file format
     * @param exporter , the exporter object
     * @param parameter , the parameters to apply
     */
    private void applyCustomParameters(Field[] fields, JRAbstractExporter< ReportExportConfiguration, ExporterConfiguration, ExporterOutput, JRExporterContext > exporter, Map<String, Object> parameters) {
        
        def fieldNames = fields.collect {it.getName()}

        parameters.each { p ->
            if (fieldNames.contains(p.getKey())) {
                def fld = Class.forName(fields.find {it.name = p.getKey()}.clazz.name).getField(p.getKey())
                // TODO mudou agora não da para colocar os parametros assim mais
                //exporter.setConfiguration(  )
                //exporter.setParameter(fld.get(fld.root.class), p.getValue())
            }
        }
    }

    /**
     * Apply the default parameters for a bunch of file format and only if useDefaultParameters is enabled.
     * @param exporter , the JRExporter
     * @param format , the target file format
     */
    private void applyDefaultParameters(JRAbstractExporter< ReportExportConfiguration, ExporterConfiguration, ExporterOutput, JRExporterContext > exporter, JasperExportFormat format) {
        switch (format) {
            case JasperExportFormat.HTML_FORMAT:
                SimpleHtmlReportConfiguration  configuration = new SimpleHtmlReportConfiguration();
                exporter.setConfiguration( configuration );
                // Não encontrei esta propriedade mais            
                // exporter.setParameter(JRHtmlExporterParameter.IS_USING_IMAGES_TO_ALIGN, false)
            break
            case JasperExportFormat.XLS_FORMAT:
                SimpleXlsReportConfiguration configuration = new SimpleXlsReportConfiguration();
                configuration.setOnePagePerSheet( true );
                configuration.setDetectCellType( true );
                configuration.setWhitePageBackground( false );
                configuration.setRemoveEmptySpaceBetweenRows( true );
                exporter.setConfiguration( configuration );
            break
            case JasperExportFormat.TEXT_FORMAT:
                SimpleTextExporterConfiguration configuration = new SimpleTextExporterConfiguration();                
                exporter.setConfiguration( configuration );
//                exporter.setParameter(JRTextExporterParameter.PAGE_WIDTH, 80)
//                exporter.setParameter(JRTextExporterParameter.PAGE_HEIGHT, 60)
//                exporter.setParameter(JRTextExporterParameter.PAGE_HEIGHT, 60)
            break
        }
    }

    /**
     * Convert a String to a Locale.
     * @param localeString , a string
     * @returns Locale
     */
    static Locale getLocaleFromString(String localeString) {
        if (localeString == null) {
            return null
        }
        localeString = localeString.trim()

        // Extract language
        int languageIndex = localeString.indexOf('_')
        String language
        if (languageIndex == -1) {  // No further "_" so is "{language}" only
            return new Locale(localeString, "")
        }
        language = localeString.substring(0, languageIndex)

        // Extract country
        int countryIndex = localeString.indexOf('_', languageIndex + 1)
        String country
        if (countryIndex == -1) {     // No further "_" so is "{language}_{country}"
            country = localeString.substring(languageIndex + 1)
            return new Locale(language, country)
        }
        // Assume all remaining is the variant so is "{language}_{country}_{variant}"
        country = localeString.substring(languageIndex + 1, countryIndex)
        String variant = localeString.substring(countryIndex + 1)
        return new Locale(language, country, variant)
    }
}
