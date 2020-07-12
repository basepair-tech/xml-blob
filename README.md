# XML Blob

XML Blob is a helper class to build an XML String using a simple interface.

```
val xmlBlob = BmlBlob.node("root",
  node("a", "key1", "value1", "key2", "value2", "1"),
  node("b", "2"),
  node("c",
    node("c1", 1),
    node("c2", 2)),
  node("d", mask("123", "***"))
)

val xml = xmlBlob.getXml() 
val maskedXml = xmlBlob.getMaskedXml()

```

Produces:
```
<root><a key1="value1" key2="value2">1</a><b>2</b><c><c1>1</c1><c2>2</c2></c><d>123</d></root>

<root><a key1="value1" key2="value2">1</a><b>2</b><c><c1>1</c1><c2>2</c2></c><d>***</d></root>
```

## Installation

Xml Blob is available from JCenter as well as Github Packages 
```
repositories {
  jcenter()
}

dependencies {
  implementation("tech.basepair:xml-blob:0.2.0")
}
```

## XML Blob API
The XML Blob API consists of several methods:

* node() 
* attrs()
* attr()
* text()
* cdata()
* mask()

### Supplier APIs
For each of the methods above there is a suppluer version which can be passed to optionally return a value.

```
val xmlNode = node("a", node { if (someValue) { node("b", 1) else { null } })
...
node("a", text { if (someValue) { text("hello") } else { null } });
```

## Printer

By default the `node()` returned from XmlBlob exposes a `getXml()` and `getMaskedXml()` methods that return the String 
representation of the XML and underlying use a default Printer that utilisies a StringBuilder.

### Masked XML
The masked XML replaces any content with the masked value supplied and the `mask()` method can take any of the XmlBlob types.

For example
```
val xmlNode = node("password", mask(text("goodbye"), "******")
xml = xmlNode.getXml() => "<password>goodbye</password>"
maskedXml = xmlBode.getMaskedXml() => "<password>******</password>"
```

### Custom Printer
A custom printer can be defined that allows outputting the xml node to an Appendable source.
For example a FilePrinter could be used to write the XML to a file.

```
class FilePrinter(_printWriter: PrintWriter, override val mask: Boolean) : XmlBlob.Printer {
  private val printWriter = _printWriter

  override fun append(p0: CharSequence?): Appendable {
    printWriter.append(p0)
    return this
  }

  override fun append(p0: CharSequence?, p1: Int, p2: Int): Appendable {
    printWriter.append(p0, p1, p2)
    return this
  }

  override fun append(p0: Char): Appendable {
    printWriter.append(p0)
    return this
  }
}

val xml = XmlBlob.node("a", mask(text("asd"), "***"))

File("path-to-file").printWriter().use { xml.appendTo(FilePrinter(it, false)) }
File("path-to-masked-file").printWriter().use { xml.appendTo(FilePrinter(it, true)) }

```

### Utilties
You can create simple utilities like the one below for creating Soap documents.
These examples could be further typed by extending the XmlBlob.Node interface.

```java
public class XmlSoapBlob {
  
  private static final XmlBlob.Attrs SOAP_ATTRS = attrs(
      attr("xmlns:soap", "http://schemas.xmlsoap.org/soap/envelope/"),
      attr("xmlns:xsi", "http://www.w3.org/1999/XMLSchema-instance"),
      attr("xmlns:xsd", "http://www.w3.org/1999/XMLSchema")
  );
    
  public static XmlBlob.Node envelope(XmlBlob.Node soapHeader, XmlBlob.Node soapBody) {
    return envelope(emptyAttrs(), soapHeader, soapBody);
  }
  
  public static XmlBlob.Node envelope(XmlBlob.Attrs additionalAtts, XmlBlob.Node soapHeader, XmlBlob.Node soapBody) {
    return node("soap:Envelope", attrs(SOAP_ATTRS, additionalAtts), soapHeader, soapBody);
  }
  
  public static XmlBlob.Node header(XmlBlob.Node ... nodes) {
    return node("soap:Header", nodes);
  }
  
  public static XmlBlob.Node body(XmlBlob.Node body) {
    return node("soap:Body", body);
  }
}
```
With the above you could then do the following:
```java
XmlBlob.Node envelope = envelope(
    header(
        node("bp:maxTime", attrs("value", "10000", "xmlns:bp", "http://basepair.tech")),
        node("bp:result", attrs("xmlns:bp", "http://basepair.tech"),
            node("bp:expected", 200)
        )
    ),
    body(
        node("getStatementResponse", attrs("xmlns", "http://basepair.tech/apis/example"),
            node("request", node("statementId", 1))
        )
    )
);
```
