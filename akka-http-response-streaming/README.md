The API's:

 * `GET finite`
 * `GET infinite`
 * `PUT infinite`
 * `GET infinite/flow`
 * `GET users`
 * `GET users/docs`


Demo `http://localhost:9027/demo` 
 
Partial results for heavy requests using `akka` and `http` chunked streams.
 
 Cons:
 * Could not find a way to join large tables.
   If there is one large table and several small ones, then this can be done.
 * Special handling of such APIs is required.
 
 Pros:
 * The API responds immediately. 
   Perhaps this will be convenient when grouping on a large table and displaying status. For example, a red / green indicator.
   
 Launch
   `sbt assembly` and `java -jar ...` on the .9 or .21 server