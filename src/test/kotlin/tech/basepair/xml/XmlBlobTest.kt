// Copyright 2020 Skiddoo Pty. Ltd.

package tech.basepair.xml;

import tech.basepair.xml.XmlBlob.Companion.attr
import tech.basepair.xml.XmlBlob.Companion.attrs
import tech.basepair.xml.XmlBlob.Companion.cdata
import tech.basepair.xml.XmlBlob.Companion.emptyAttr
import tech.basepair.xml.XmlBlob.Companion.emptyAttrs
import tech.basepair.xml.XmlBlob.Companion.emptyCdata
import tech.basepair.xml.XmlBlob.Companion.emptyNode
import tech.basepair.xml.XmlBlob.Companion.emptyText
import tech.basepair.xml.XmlBlob.Companion.mask
import tech.basepair.xml.XmlBlob.Companion.node
import tech.basepair.xml.XmlBlob.Companion.text
import kotlin.test.Test
import kotlin.test.assertEquals

class XmlBlobTest {

  @Test
  fun testSimpleNode() {
    val xmlNode = node("a")
    assertEquals("<a/>", xmlNode.getXml())
  }

  @Test
  fun testChildNodes() {
    val xmlNode = node("a", node("b"), node("c"))
    assertEquals("<a><b/><c/></a>", xmlNode.getXml())
  }

  @Test
  fun testMultipleChildNodeDepth() {
    val xmlNode = node("a", node("b", node("c", node("d"))))
    assertEquals("<a><b><c><d/></c></b></a>", xmlNode.getXml())
  }

  @Test
  fun testNodeWithAttributes() {
    val xmlNode = node("a", attrs("k1", "v1", "k2", "v2"))
    assertEquals("""<a k1="v1" k2="v2"/>""", xmlNode.getXml())
  }

  @Test
  fun testEmptyXml() {
    val xmlNode = node("a", emptyNode())
    assertEquals("<a></a>", xmlNode.getXml())
  }

  @Test
  fun testEmptyText() {
    val xmlNode = node("a", emptyText())
    assertEquals("<a></a>", xmlNode.getXml())
  }

  @Test
  fun testEmptyAttr() {
    val xmlNode = node("a", attrs(attr("a", "1"), emptyAttr()), "hello")
    assertEquals("""<a a="1">hello</a>""", xmlNode.getXml())
  }

  @Test
  fun testEmptyAttrs() {
    val xmlNode = node("a", emptyAttrs(), "hello")
    assertEquals("<a>hello</a>", xmlNode.getXml())
  }

  @Test
  fun testEmptyCdata() {
    val xmlNode = node("a", emptyCdata())
    assertEquals("<a></a>", xmlNode.getXml())
  }

  @Test
  fun testNodeChildrenAsList() {
    val xmlNode = node("a", listOf(node("b"), node("c")))
    assertEquals("<a><b/><c/></a>", xmlNode.getXml())
  }

  @Test
  fun testBooleanNode() {
    val xmlNode = node("a", true)
    assertEquals("<a>true</a>", xmlNode.getXml())
  }

  @Test
  fun testIntNode() {
    val xmlNode = node("a", 1)
    assertEquals("<a>1</a>", xmlNode.getXml())
  }

  @Test
  fun testSupplierNodePresent() {
    val xmlNode = node("a", node { node("b", "hello") })
    assertEquals("<a><b>hello</b></a>", xmlNode.getXml())
  }

  @Test
  fun testAddingMoreAttributes() {
    val attrs = attrs("k1", "v1", "k2", "v2", "k3", "v3")
    val xmlNode = node("a", attrs(attrs, attr("k4", "v4"), attr("k0", "v0"), attr("k1", "v1-1")))
    assertEquals("""<a k0="v0" k1="v1-1" k2="v2" k3="v3" k4="v4"/>""", xmlNode.getXml())
  }

  @Test
  fun testCombiningTwoAttributes() {
    val attrs = attrs("k1", "v1", "k2", "v2", "k3", "v3")
    val attrs2 = attrs("k4", "v4", "k5", "v5", "k1", "v1-1")
    val xmlNode = node("a", attrs(attrs, attrs2))
    assertEquals("""<a k1="v1-1" k2="v2" k3="v3" k4="v4" k5="v5"/>""", xmlNode.getXml())
  }

  @Test
  fun testSupplierNodeNotPresent() {
    val someValue = false
    val xmlNode = node("a", node { if (someValue) { node("b", "hello") } else { null } })
    assertEquals("<a></a>", xmlNode.getXml())
  }

  @Test
  fun testSupplierAttrsNotPresent() {
    val someValue = false
    val xmlNode = node("a", attrs { if (someValue) { attrs(attr("a", "b")) } else { null } })
    assertEquals("<a/>", xmlNode.getXml())
  }

  @Test
  fun testSupplierAttrNotPresent() {
    val someValue = false
    val xmlNode = node("a", attrs(attr("k1", "v1"), attr { if (someValue) { attr("k2", "v2") } else { null } }))
    assertEquals("""<a k1="v1"/>""", xmlNode.getXml())
  }

  @Test
  fun testSupplerTextNotPresent() {
    val someValue = false
    val xmlNode = node("a", text { if (someValue) { text("hello") } else { null } })
    assertEquals("""<a></a>""", xmlNode.getXml())
  }

  @Test
  fun testSupplerCdataNotPresent() {
    val someValue = false
    val xmlNode = node("a", cdata { if (someValue) { cdata("hello") } else { null } })
    assertEquals("""<a></a>""", xmlNode.getXml())
  }

  @Test
  fun testMaskText() {
    val xmlNode = node("a", mask(text("hello"), "***"))
    assertEquals("<a>hello</a>", xmlNode.getXml())
    assertEquals("<a>***</a>", xmlNode.getMaskedXml(), "masked xml does not match")
  }

  @Test
  fun testMaskStr() {
    val xmlNode = node("a", mask("hello", "***"))
    assertEquals("<a>hello</a>", xmlNode.getXml())
    assertEquals("<a>***</a>", xmlNode.getMaskedXml(), "masked xml does not match")
  }

  @Test
  fun testMaskNode() {
    val xmlNode = node("a", mask(node("b", attrs("key", "value")), "***"))
    assertEquals("""<a><b key="value"/></a>""", xmlNode.getXml())
    assertEquals("<a><***/></a>", xmlNode.getMaskedXml())
  }

  @Test
  fun testMaskCdata() {
    val xmlNode = node("a", mask(cdata("Hello"), "***"))
    assertEquals("<a><![CDATA[Hello]]></a>", xmlNode.getXml())
    assertEquals("<a><![CDATA[***]]></a>", xmlNode.getMaskedXml())
  }

  @Test
  fun testMaskAttributes() {
    val xmlNode = node("a", attrs(mask(attr("key", "value"), "***")), "hello")
    assertEquals("""<a key="value">hello</a>""", xmlNode.getXml())
    assertEquals("""<a key="***">hello</a>""", xmlNode.getMaskedXml())
  }
}