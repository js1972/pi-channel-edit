package au.com.jaylin.pi.channel

import akka.actor.Actor
import spray.routing._
import spray.http._
import spray.http.StatusCodes._
import spray.routing.authentication._
import scala.concurrent.Future
import spray.util.LoggingContext
import MediaTypes._
import spray.json._
import DefaultJsonProtocol._


/**
 * Access locally via http://localhost:8888/zpi-channel-edit/
 * Read request: http://localhost:8888/zpi-channel-edit/channel/:BC_Jason:ERP_PurchaseOrders_Test_R_FILE
 * Update request: http://localhost:8888/zpi-channel-edit/channel/:BC_Jason:ERP_PurchaseOrders_Test_R_FILE?param=file.targetDir&value=new_value
 * 
 * We don't implement our route structure directly in the service actor because
 * we want to be able to test it independently, without having to spin up an actor.
 */
class RestServiceActor extends Actor with RestService {

    // the HttpService trait defines only one abstract member, which
    // connects the services environment to the enclosing actor or test
    def actorRefFactory = context

    // this actor only runs our route, but you could add
    // other things here, like request stream processing
    // or timeout handling
    def receive = runRoute(serviceRoute)
}


/**
 * This trait defines our service behavior independently from the service actor. 
 */
trait RestService extends HttpService {
    implicit def executionContext = actorRefFactory.dispatcher
    
    // Setup Spray basic auth. See for details:
    // http://spray.io/documentation/1.1-SNAPSHOT/spray-routing/security-directives/authenticate/
    def myUserPassAuthenticator(userPass: Option[UserPass]): Future[Option[String]] =
        Future {
            if (userPass.exists(up => true)) {
                Some(userPass.get.user + ":" + userPass.get.pass)
            }
            else None
            //if (userPass.exists(up => up.user == "jason" && up.pass == "test")) Some(userPass.get.user + ":" + userPass.get.pass)
            //else None
        }
    
    // Spray exception handling
    implicit def myExceptionHandler(implicit log: LoggingContext) =
        ExceptionHandler {
            case e: Exception =>
                requestUri { uri =>
                    log.warning("Request to {} could not be handled normally", uri)
                    complete(InternalServerError, "Oops! Something went wrong!!!\n(Note: PI web service errors such as authentication are reported as http500.)\n\n" + e.getMessage())
        }
    }
    
    
    val serviceRoute =
        path("channel" / PathElement) { id =>
            authenticate(BasicAuth(myUserPassAuthenticator _, realm = "API Access")) { userPass =>
                get {
                    val idParts = id.split(":")
                    if (idParts.length < 3) complete(InternalServerError, "Invalid comm.channel read request. Enter request as http:<server>:<port>/channel/<party>:<component>:<channelId>")
                    else if (idParts(2).isEmpty()) complete(InternalServerError, "Invalid comm.channel read request. Enter request as http:<server>:<port>/channel/<party>:<component>:<channelId>")
                    else {
                        respondWithMediaType(`application/xml`) {
                            complete {
                                val c = new Channel(idParts(0), idParts(1), idParts(2), userPass)  //parts = Party, Component, ChannelID
                                c.read
                            }
                        }
                    }
                }
            } ~
            authenticate(BasicAuth(myUserPassAuthenticator _, realm = "API Access")) { userPass =>
                put {
                    val idParts = id.split(":")
                    if (idParts.length < 3) complete(InternalServerError, "Invalid comm.channel read request. Enter request as http:<server>:<port>/channel/<party>:<component>:<channelId>?param=<parameter>&value=<value>")
                    else if (idParts(2).isEmpty()) complete(InternalServerError, "Invalid comm.channel read request. Enter request as http:<server>:<port>/channel/<party>:<component>:<channelId>?param=<parameter>&value=<value>")
                    else {
                        parameters('param, 'value) { (paramToChange, newValue) =>
                            if (paramToChange.isEmpty() || newValue.isEmpty()) {
                                complete("param and value querystring parameters not supplied! Enter request as http:<server>:<port>/channel/<party>:<component>:<channelId>?param=<parameter>&value=<value>")
                            }
                            else {
                                respondWithMediaType(`application/xml`) {
                                    complete {
                                        val c = new Channel(idParts(0), idParts(1), idParts(2), userPass)
                                        val readResult = c.read
                                        val (updateResult, oldValue) = c.update(readResult.get, paramToChange, newValue)
                                        if (updateResult.isDefined) {
                                            val activationResult = c.activate
                                            if (activationResult.isDefined && activationResult.get.equals("Success")) {
                                                <results>
                                                    <previous>{ oldValue }</previous>
                                                </results>
                                            } else {
                                                <results>{ activationResult }</results>
                                            }
                                        }
                                        else "ERROR: Nothing returned from update()."
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } ~
        // Handle invalid URL's below...
        pathPrefix("channel") {
            pathSingleSlash {
                complete("Which channel? Enter request as http:<server>:<port>/channel/<party>:<component>:<channelId>")
            }
        } ~
        path("channel") {
            complete("Which channel? Enter request as http:<server>:<port>/channel/<party>:<component>:<channelId>")
        } ~
        pathSingleSlash {
            get {
                redirect("/zpi-channel-edit/channel", StatusCodes.MovedPermanently)
            }
        }
}