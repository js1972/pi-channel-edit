pi-channel-edit
===============

Scala RESTful API for editing PI Communication Channels

This project is a work-in-progress that enables a http client to change SAP PI Communication Channels. The intent is for it to be used for automated regression testing using soapUI.
Within a soapUI test set we can:
-- call pi-channel-edit to stub out the interface channels
-- run our interface test
-- call pi-channel-edit to un-stub the channels back to how they were.
