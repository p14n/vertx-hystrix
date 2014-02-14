vertx-hystrix
=============

An example project showing Hystrix and Vert.x working together.  Check out and run.sh (or just examine the contents to see what needs running). When running, the verticle in the sender project starts sending messages to the receiver project.  Load up VisualVM with the mbeans plugin to see the Hystrix metrics. 

Contains Hystrix commands running in in three different ways

 * As a registered handler, using HystrixNonBlockingCommand (receiver project)
 * As a registered handler, using HystrixCommand which will use the Hystrix thread pool (receiver project)
 * As a sender (sender project)
 
Note this uses unreleased versions of both Hystrix and mod-rxvertx (in the libs directory).
