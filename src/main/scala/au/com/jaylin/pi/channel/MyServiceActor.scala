package au.com.jaylin.pi.channel

import akka.actor.Actor
import spray.routing._
import spray.http._
import spray.http.StatusCodes._
import MediaTypes._
import spray.json._
import DefaultJsonProtocol._


/**
 * Access locally via http://localhost:8888/zpi-channel-edit/
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

    val serviceRoute =
        path("") {  //TESTING
            get {
                respondWithMediaType(`text/html`) { // XML is marshalled to `text/xml` by default, so we simply override here
                    complete {
                        <html>
                            <body>
                                <h1>This is a REST service implemented with <i>spray-routing</i>!</h1>
                            </body>
                        </html>
                    }
                }
            }
        } ~
        path("json") { //TESTING
            respondWithMediaType(`application/json`) {
                complete {
                    val v = Map("x" -> 24, "y" -> 25, "z" -> 26)
                    v.toJson.prettyPrint
                }
            }
        } ~
        path("channel" / PathElement) { id =>
            get {
                val idParts = id.split(":")
                if (idParts.length < 3) complete(InternalServerError, "Invalid comm.channel read request. Enter request as http:<server>:<port>/channel/<party>:<component>:<channelId>")
                else if (idParts(2).isEmpty()) complete(InternalServerError, "Invalid comm.channel read request. Enter request as http:<server>:<port>/channel/<party>:<component>:<channelId>")
                else {
                    respondWithMediaType(`application/xml`) {
                        complete {
                            val c = new MailChannel(idParts(0), idParts(1), idParts(2))  //Party, Component, ChannelID
                            c.read
                        }
                    }
                }
            } ~
            put {
                val idParts = id.split(":")
                if (idParts.length < 3) complete(InternalServerError, "Invalid comm.channel read request. Enter request as http:<server>:<port>/channel/<party>:<component>:<channelId>")
                else if (idParts(2).isEmpty()) complete(InternalServerError, "Invalid comm.channel read request. Enter request as http:<server>:<port>/channel/<party>:<component>:<channelId>")
                else {
                    respondWithMediaType(`application/xml`) {
                        complete {
                            val c = new MailChannel("", "BC_Jason", "ERP_PurchaseOrders_Test_R_FILE")
                            val readResult = c.read
                            val (updateResult, oldValue) = c.update(readResult.get, "file.targetDir", "XXXYYYZZZ")
                            if (updateResult.isDefined) updateResult.get else "ERROR: Nothing returned from update()."
                        }
                    }
                }
            }
        } ~
        path("channel" /) {
            complete("Which channel? Enter request as http:<server>:<port>/channel/<party>:<component>:<channelId>")
        } ~
        path("channel") {
            complete("Which channel? Enter request as http:<server>:<port>/channel/<party>:<component>:<channelId>")
        }
}