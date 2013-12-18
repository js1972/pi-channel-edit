package au.com.jaylin.pi.channel

import scala.xml.transform.RewriteRule
import scala.xml.Node
import scala.xml.Elem
import scala.xml.transform.RuleTransformer
import scala.xml.NodeSeq


trait Channel {
    var channelParty = ""
    var channelComponent = ""
    var channelId: String = ""

    /**
     * Read the comm.channel details and return the CommunicationChannel element.
     */
    def read: Option[scala.xml.NodeSeq] = {
        val client = new SoapClient
        val req  = <pns:read xmlns:pns="urn:CommunicationChannelServiceVi">
                       <yq1:CommunicationChannelReadRequest xmlns:yq1="urn:CommunicationChannelServiceVi" xmlns:pns="urn:com.sap.aii.ibdir.server.api.types">
                           <pns:CommunicationChannelID>
                               <pns:PartyID>{channelParty}</pns:PartyID>
                               <pns:ComponentID>{channelComponent}</pns:ComponentID>
                               <pns:ChannelID>{channelId}</pns:ChannelID>
                           </pns:CommunicationChannelID>
                       </yq1:CommunicationChannelReadRequest>
                   </pns:read>

        val resp = client.sendMessage("http://app1poy.inpex.com.au:58000/CommunicationChannelService/HTTPBasicAuth?style=document", req, "jscott", "sophie05")
        if (resp.isDefined) {
            val nodes = (resp.get \\ "CommunicationChannel")
            Some(nodes)
        }
        else None
    }
    
    /**
     * Given an XML Node Sequence, find the old value that is about to be replaced.
     */
    def findValueGivenName(xml: NodeSeq, name: String): String = {
        val components = xml \ "AdapterSpecificAttribute" \ "_"                    //get all the <name><value> children
        val index = components.zipWithIndex find (_._1.text == name) map (_._2)    //zip the name/value elements up into tuples and find the one we want
        val oldValue = index map (_ + 1) map components                            //extract the second element of the found tuple
        if (oldValue.isDefined) oldValue.get.text else ""
    }
    
    def update(xmlNodes: scala.xml.NodeSeq, param: String, value: String): (Option[scala.xml.NodeSeq], String)
}

class MailChannel(val party: String, val component: String, val id: String) extends Channel {
    channelParty = party
    channelComponent = component
    channelId = id
    
    /**
     * Update the comm.channel switching the required parameter - returns the XML NodeSeq containing
     * the ChangeListID as well as the old value which was replaced.
     */
    def update(xmlNodes: scala.xml.NodeSeq, paramToChange: String, valueToChange: String): (Option[scala.xml.NodeSeq], String) = {
      
        // Here we define a transform to update the xml. We need to find the relevant Name elements
        // and change the sibling Value element.
        // We need to match on the encompassing AdapterSpecificAttribute elements and then play with
        // the children nodes.
        // 1. For each AdapterSpecificAttribute
        // 2. Find any Name nodes (children.find)
        // 3. For each (collect) of the children apply the partial function (case matching) to translate from Value to the new Value
        //    if a Name was found above
        // 4. Wrap the content in the encompassing tag
        //
        // This logic was worked out with the help of this SO post:
        // http://stackoverflow.com/questions/16546205/transforming-an-xml-element-based-on-its-sibling-using-scala-xml-transform-rule
      
        val rule = new RewriteRule {            
            override def transform(n: Node): Seq[Node] = n match {
                case el @  Elem(_, "AdapterSpecificAttribute", _, _, _*) =>
                    val children = el.child
                    //val name = children.find(_.label == "Name")
                    val name = children.find(p => p.label == "Name" && p.text == paramToChange)
                    val content = children.collect {
                      case el @ Elem(_, "Value", _, _, _*) =>
                          //<Value>{el.text + name.map(_.text).getOrElse("")}</Value>
                          <Value>{ if (name.map(_.text).isDefined) valueToChange else el.text }</Value>
                      case e:Elem => e
                    }
                    <AdapterSpecificAttribute>{content}</AdapterSpecificAttribute>
                    
                case other => other
            }
        }

        val transformed = new RuleTransformer(rule).transform(xmlNodes)

        val req = <pns:change xmlns:pns="urn:CommunicationChannelServiceVi">
                      <yq1:CommunicationChannelChangeRequest xmlns:yq1="urn:CommunicationChannelServiceVi" xmlns:pns="urn:com.sap.aii.ibdir.server.api.types">
                          {transformed}
                      </yq1:CommunicationChannelChangeRequest>
                  </pns:change>

        val oldValue = findValueGivenName(xmlNodes, "file.targetDir")
        
        val client = new SoapClient
        val resp = client.sendMessage("http://app1poy.inpex.com.au:58000/CommunicationChannelService/HTTPBasicAuth?style=document", req, "jscott", "sophie05")
        
        if (resp.isDefined) {
            val nodes = (resp.get \\ "ChangeListID")
            (Some(nodes), oldValue)
        }
        else (None, oldValue)
    }
}

class SoapChannel(val id: String) extends Channel {
    def update(xmlNodes: scala.xml.NodeSeq, param: String, value: String): (Option[scala.xml.NodeSeq], String) = {
        (None, "")
    }
}

object Test {
    def main(args: Array[String]) {
        println("Start...")
        
        println("Reading comm.channel...")
        val c = new MailChannel("", "BC_Jason", "ERP_PurchaseOrders_Test_R_FILE")
        val readResult = c.read
        
        println("updating comm.channel...")
        val (updateResult, oldValue) = c.update(readResult.get, "file.targetDir", "XXXYYYZZZ")
        println(if (updateResult.isDefined) updateResult.get else "ERROR: Nothing returned from update().")
        println("\nDone.")
    }
}