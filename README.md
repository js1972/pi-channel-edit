pi-channel-edit
===============

####Scala RESTful API for editing PI Communication Channels

This project is a work-in-progress that enables a http client to change SAP PI Communication Channels. The intent is for it to be used for automated regression testing of PI interfaces using soapUI.
Within a soapUI test set we can:
-- call pi-channel-edit to stub out the interface channels
-- run our interface test
-- call pi-channel-edit to un-stub the channels back to how they were.

When setting up a test case in soapUI the first step is to insert a REST call to this service:
    http://localhost:8888/zpi-channel-edit/channel/:BC_Jason:ERP_PurchaseOrders_Test_R_FILE?param=file.targetDir&value=folder

Format of final URL component: <Party>:<Component>:<ChannelID>
Querystring: param is the communicaiton channel parameter you wish to change (mock); value is the new value.

This call returns the old value which can be saved and swapped back in at the end of the soapUI test run.

A list of available comm.channel parameter codes can be seen by executing service:
    http://localhost:8888/zpi-channel-edit/channel/:BC_Jason:ERP_PurchaseOrders_Test_R_FILE

Both the above REST services rely on BASIC authentication. This has currently been implemented to simply fall through to the PI web service for authentication.

###Configuration
You must add an application.conf file in src/main/resources which includes the following (its hidden so as to not show our pi server):

    akka {
      loglevel = INFO
    }
    
    spray.servlet {
      boot-class = "au.com.jaylin.pi.channel.Boot"
      request-timeout = 30s
      
      ## The full path from the Servlet container root where Spray should handle
      ## requests.  If you package your web-application into foobar.war, then
      ## the resulting "context" is /foobar when the servlet container unpacks
      ## your application.  And so, Spray will handle requests under
      ## /foobar/app* and /foobar/app/*.  Note this is tied closely with
      ## the SprayConnectorServlet's servlet-mapping URL pattern set in your
      ## web.xml.
      root-path = "/zpi-channel-edit"
    }
    
    config {
      host="<hostname of PI Server>:<port>"
    }

###Build and Execute
1. Clone the repo and cd into the top-level directory
2. Add the application.conf file to src/main/resources
3. Run sbt (all dependancie will download) and then enter container:start at the sbt command prompt
4. Access the api via your browser or favourite rest client or soapUI for testing a PI interface.

### Execute in standalone container (ie Tomcat)
1. Download zpi-channel-edit.war from blah
2. Drop the war file into Tomcats webapps directory and start Tomcat. The war file will be expanded into its own directory
3. Navigate to src/main/resources/application.conf and edit the target PI server hostname to match your PI server. Restart Tomcat
4. You can now access the api at http://localhost:8081/zpi-channel-edit/channel.

