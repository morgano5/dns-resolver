# DNS Resolver

A non blocking DNS resolver for Java.

This is a snippet of how to use this to get all type of RRs from a dns name (just while I write a more proper doc):

```java

Resolver resolver = new Resolver();

Resolver.AnswerProcess process = resolver.lookup("yahoo.com", DnsType.ALL);

int timeout = 100;
boolean done;
do {

    done = process.doIO(timeout); // It only blocks for 'timeout' milliseconds, returns true if there is an outcome

    // do something else...

} while (!done);

List<ResourceRecord> result = process.getResult();

for(ResourceRecord rr: result) {
    System.out.println("Result: " + rr.getDnsName() + " - " + rr.getDnsType() + " - " + rr.getData(Object.class));
}
```

This is still work in progress. Basic functionality is already there but there are some issues to fix.

## TODO list

- [ ] Write documentation
- [ ] Restart queries when a CNAME RR is reached
- [ ] Support for other RRs specified in DNS updates and extensions
- [ ] Support for MAILB, AXFR and other possible queries returning more than one type of RR
- [ ] Support for IPv6

## To consider

* Support classes other than __IN__
* Support inverse queries

