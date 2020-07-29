/*
 * Copyright 2020 Base Pair Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and limitations under the License.
 *
 */
package tech.basepair.xml

import kotlinx.collections.immutable.persistentHashSetOf
import org.apache.commons.text.StringEscapeUtils

/**
 * Utility for building a String that represents an XML document.
 * The XmlBlob object is immutable and the passed values are copied where it makes sense.
 *
 * Note the XML Declaration is not output as part of the XmlBlob
 *
 *    val soapAttrs = attrs("soap-env" to "http://www.w3.org/2001/12/soap-envelope",
 *        "soap-env:encodingStyle" to "http://www.w3.org/2001/12/soap-encoding")
 *    val soapXml = node("soap-env:Envelope", soapAttrs,
 *        node("soap-env:Body", attrs("xmlns:m" to "http://basepair.tech/example"),
 *            node("m:GetExample",
 *                node("m:GetExampleName", "BasePair")
 *            )
 *        )
 *    )
 */
sealed class XmlBlob {

  /**
   * Interface to represent objects that are serializable using the [appendTo] method
   */
  interface Blob {
    /**
     * Serializes the XML out to a Printer
     */
    fun appendTo(printer: Printer) {}
  }

  /**
   * Represents an xml element
   */
  interface Node : Blob {
    @JvmDefault fun toXml(shouldMask: Boolean = false) = toXml(StringBuilderPrinter(shouldMask))

    @JvmDefault fun toXml(printer: Printer): String {
      appendTo(printer)
      return printer.toString()
    }
  }

  /**
   * Represents the attribute list on an [Node]
   */
  interface Attrs : Blob {
    val attrs: Set<Attr>
  }

  /**
   * Represents the attribute in an attribute list [Attrs]
   */
  interface Attr : Blob {
    val key: String
    val value: String
  }

  /**
   * Represents the text of an xml element [Node]
   */
  interface Text : Blob

  /**
   * Represents the CDATA of an xml element [Node]
   */
  interface Cdata : Blob

  companion object {
    private val EMPTY_NODE : Node = EmptyNode()
    private val EMPTY_TEXT : Text = EmptyText()
    private val EMPTY_ATTRS : Attrs = EmptyAttrs()
    private val EMPTY_ATTR : Attr = EmptyAttr()
    private val EMPTY_CDATA : Cdata = EmptyCdata()

    /**
     * Creates an empty [Node] that when serialized is empty
     */
    @JvmStatic fun emptyNode() = EMPTY_NODE
    /**
     * Creates an empty [Text] that when serialized is empty
     */
    @JvmStatic fun emptyText() = EMPTY_TEXT
    /**
     * Creates an empty [Attr] that when serialized is empty
     */
    @JvmStatic fun emptyAttr() = EMPTY_ATTR
    /**
     * Creates an empty [Attrs] that when serialized is empty
     */
    @JvmStatic fun emptyAttrs() = EMPTY_ATTRS
    /**
     * Creates an empty [Cdata] that when serialized is empty
     */
    @JvmStatic fun emptyCdata() = EMPTY_CDATA

    /**
     * Creates a node with the given contents as children nodes.
     * This is equivalent to calling [node{String, Attrs, vararg Node} with [emptyAttrs]
     */
    @JvmStatic
    fun node(tag: String, vararg contents: Node): Node = node(tag, EMPTY_ATTRS, *contents)

    /**
     * Creates a node with the given list of contents as children nodes
     * This is equivalent to calling [node{String, Attrs, List<Node>}] with [emptyAttrs]
     */
    @JvmStatic
    fun node(tag: String, contents: List<Node>): Node = node(tag, EMPTY_ATTRS, contents)

    /**
     * Creates a node with the given attributes and children contents
     */
    @JvmStatic
    fun node(tag: String, attrs: Attrs, vararg contents: Node): Node = NodeImpl(tag, attrs, *contents)

    /**
     * Creates a node with the given attributes and list of contents as children nodes
     */
    @JvmStatic
    fun node(tag: String, attrs: Attrs, contents: List<Node>): Node = NodeImpl(tag, attrs, contents)

    /**
     * Creates a node with a CDATA child node.
     * This is equivalent to calling [node{String, Attrs, Cdata}] with [emptyAttrs]
     */
    @JvmStatic
    fun node(tag: String, cdata: Cdata): Node = node(tag, EMPTY_ATTRS, cdata)

    /**
     * Creates a node with the give attributes and CDATA child node
     */
    @JvmStatic
    fun node(tag: String, attrs: Attrs, cdata: Cdata): Node = NodeImpl(tag, attrs, cdata)

    /**
     * Creates a node with the given text child node
     * This is equivalent to calling [node{String, Attrs, Text}] with [emptyAttrs]
     */
    @JvmStatic
    fun node(tag: String, text: Text): Node = node(tag, EMPTY_ATTRS, text)

    /**
     * Creates a node with the child node that contains the value of the integer
     * This is equivalent to calling [node{String, Attrs, Int}] with [emptyAttrs]
     */
    @JvmStatic
    fun node(tag: String, num: Int): Node = node(tag, EMPTY_ATTRS, num)

    /**
     * Creates a node with the child node that contains the value of the boolean
     * This is equivalent to calling [node{String, Attrs, Boolean}] with [emptyAttrs]
     */
    @JvmStatic
    fun node(tag: String, value: Boolean): Node = node(tag, EMPTY_ATTRS, value)

    /**
     * Creates a node with the child node that contains the value of the string
     * This is equivalent to calling [node{String, Attrs, String}] with [emptyAttrs]
     */
    @JvmStatic
    fun node(tag: String, value: String): Node = node(tag, EMPTY_ATTRS, value)

    /**
     * Creates a node with the child node that contains the value of the integer
     */
    @JvmStatic
    fun node(tag: String, attrs: Attrs, num: Int): Node = NodeImpl(tag, attrs, text(num.toString()))

    /**
     * Creates a node with the child node that contains the value of the boolean
     */
    @JvmStatic
    fun node(tag: String, attrs: Attrs  = emptyAttrs(), value: Boolean): Node = node(tag, attrs, text(value.toString()))

    /**
     * Creates a node with the child node that contains the value of the string
     * This is equivalent to calling [node{String, Attrs, Text}] with [Text]
     */
    @JvmStatic
    fun node(tag: String, attrs: Attrs, value: String): Node = node(tag, attrs, text(value))

    /**
     * Creates a node with the given text child node
     */
    @JvmStatic
    fun node(tag: String, attrs: Attrs, text: Text): Node = NodeImpl(tag, attrs, text)

    /**
     * Creates a set of attributes based on the strings.
     * The number of strings must be even to ensure each key has a value.
     *
     */
    @JvmStatic
    fun attrs(vararg pairs: String): Attrs {
      check(pairs.size % 2 == 0) { "attribute pairs must be even" }
      val attrs = Array(pairs.size / 2) { i -> attr(pairs[i * 2], pairs[i * 2 + 1]) }
      return attrs(*attrs)
    }

    /**
     * Creates a Set if attributes based on the list of attributes provided
     */
    @JvmStatic
    fun attrs(vararg pairs: Attr): Attrs = AttrsImpl(*pairs)

    /**
     * Merges the Set of Attributes with tho other attributes provided
     * Note the second set of atributes takes precendence and will overwrite those in the first.
     *
     * This is equivalent to calling [attrs{Attrs, Attrs}] instead of the list of Attr
     */
    @JvmStatic
    fun attrs(attrs: Attrs, vararg otherAttrs: Attr): Attrs {
      return attrs(attrs, attrs(*otherAttrs))
    }

    /**
     * Merges a set of attributes with a list of attributes
     * attributes in otherAttributes will overwrite values in the original attributes
     */
    @JvmStatic
    fun attrs(attrs: Attrs, vararg otherAttrs: Attrs): Attrs {
      var combined: Set<Attr> = attrs.attrs
      for (a in otherAttrs) {
        combined = combined.subtract(a.attrs).union(a.attrs)
      }
      return AttrsImpl(combined)
    }

    /**
     * Creates an attributes with the provided key and value
     */
    @JvmStatic
    fun attr(key: String, value: String): Attr = AttrImpl(key, value)

    /**
     * Creates a text node with the supplied value
     */
    @JvmStatic
    fun text(text: String): Text = TextImpl(text)

    /**
     * Creates a cdata node with the supplied value
     */
    @JvmStatic
    fun cdata(value: String): Cdata = CdataImpl(value)

    /**
     * Creates a masked node which when the masked version is printed the value of the node will be replaced
     * with the mask value.
     *
     *    // Below results in: <masked/> instead of <a>value</a> when masked xml is requested.
     *    val maskedNode = mask(node("a", text("value"), "masked")
     *
     * @param node The node to mask or output
     * @param mask The mask value. Defaults to `***masked***`
     * @see [Node.toXml]
     */
    @JvmStatic
    @JvmOverloads
    fun mask(node: Node, mask: String = "***masked***"): Node = MaskedNode(node, mask)

    /**
     * Creates a masked attribute which when the masked version is printed the value of the attribute will be replaced
     * with the masked value.
     *
     *    // Below results in: <a key="masked">text</a> instead of <a key="value">text</a> when masked xml is requested.
     *    val maskedNode = node("a", attrs(mask(attr("key", "value"), "masked")), text("text"))
     *
     * @param attr The attribute with the value to mask
     * @param mask The mask value. Defaults to `***masked***`
     * @see [Node.toXml]
     */
    @JvmStatic
    @JvmOverloads
    fun mask(attr: Attr, mask: String = "***masked***"): Attr = MaskedAttr(attr, mask)

    /**
     * Creates a masked text which when the masked version is printed the value of the text will be replaced
     * with the masked value.
     *
     *    // Below results in: <a>masked</a> instead of <a>text</a> when masked xml is requested.
     *    val maskedNode = node("a", mask(text("text"), "masked"))
     *
     * @param text The text to mask
     * @param mask The mask value. Defaults to `***masked***`
     * @see [Node.toXml]
     */
    @JvmStatic
    fun mask(text: Text, mask: String = "***masked***"): Text = MaskedText(text, mask)

    /**
     * Creates a masked text which when the masked version is printed the value of the text will be replaced
     * with the masked value.
     *
     *    // Below results in: <a>masked</a> instead of <a>text</a> when masked xml is requested.
     *    val maskedNode = node("a", mask("text", "masked"))
     *
     * This is equivalent to calling [mask{Text, String}] with the contents wrapped in `text()`
     *
     * @param content The text to mask
     * @param mask The mask value. Defaults to `***masked***`
     * @see [Node.toXml]
     */
    @JvmStatic
    @JvmOverloads
    fun mask(content: String, mask: String = "***masked***"): Text = MaskedText(text(content), mask)

    /**
     * Creates a masked cdata which when the masked version is printed the contents of the cdata will be replaced
     * with the masked value.
     *
     *    // Below results in: <a><![CDATA[***masked***]]></a> instead of <a><![CDATA[text]]></a> when masked xml is requested.
     *    val maskedNode = node("a", mask(cdata("text")))
     *
     * @param cdata The cdata value to mask
     * @param mask The mask value. Defaults to `***masked***`
     * @see [Node.toXml]
     */
    @JvmStatic
    @JvmOverloads
    fun mask(cdata: Cdata, mask: String = "***masked***"): Cdata = MaskedCdata(cdata, mask)

    /**
     * Allows passing a node supplier that can be null.
     * If the returned node from the supplier is missing then an empty node is returned.
     */
    @JvmStatic
    fun node(supplier: () -> Node?) = supplier() ?: emptyNode()

    /**
     * Allows passing a attributes supplier that can be null.
     * If the returned attributes from the supplier are missing then an empty attributes object is returned.
     */
    @JvmStatic
    fun attrs(supplier: () -> Attrs?) = supplier() ?: emptyAttrs()

    /**
     * Allows passing a node supplier that can be null.
     * If the returned node from the supplier is missing then an emptyNode is returned.
     */
    @JvmStatic
    fun attr(supplier: () -> Attr?) = supplier() ?: emptyAttr()

    /**
     * Allows passing a text supplier that can be null.
     * If the returned text from the supplier is missing then an empty text object is returned.
     */
    @JvmStatic
    fun text(supplier: () -> Text?) = supplier() ?: emptyText()

    /**
     * Allows passing a cdata supplier that can be null.
     * If the returned cdata from the supplier is missing then an empty cdata object is returned.
     */
    @JvmStatic
    fun cdata(supplier: () -> Cdata?) = supplier() ?: emptyCdata()
  }


  private class EmptyNode : Node
  private class EmptyAttr : Attr {
    override val key: String
      get() = ""
    override val value: String
      get() = ""
  }

  private class EmptyAttrs : Attrs {
    override val attrs: Set<Attr>
      get() = HashSet()
  }

  private class EmptyCdata : Cdata
  private class EmptyText : Text

  private class NodeImpl : Node {
    private val tag: String
    private val attrs: Attrs
    private val children: List<Blob>

    internal constructor(_tag: String, _attrs: Attrs, _children: List<Blob>) {
      this.tag = _tag
      this.attrs = _attrs
      this.children = ArrayList<Blob>(_children)
    }

    internal constructor(_tag: String, _attrs: Attrs, vararg _children: Blob) {
      this.tag = _tag
      this.attrs = _attrs
      this.children = listOf(*_children)
    }

    override fun appendTo(printer: Printer) {
      printer.append('<').append(tag)

      attrs.appendTo(printer)

      if (children.isEmpty()) {
        printer.append("/>")
      } else {
        printer.append(">")
        children.forEach {
          it.appendTo(printer)
        }
        printer.append("</").append(tag).append(">")
      }
    }
  }

  private class TextImpl(_text: String) : Text {
    private val text = _text

    override fun appendTo(printer: Printer) {
      printer.append(StringEscapeUtils.escapeXml11(text))
    }
  }

  private class CdataImpl(_value: String) : Cdata {
    private val value = _value

    override fun appendTo(printer: Printer) {
      printer.append("<![CDATA[")
          .append(value)
          .append("]]>")
    }
  }

  private class AttrsImpl : Attrs {
    override val attrs: Set<Attr>

    internal constructor(vararg _attrs: Attr) {
      this.attrs = persistentHashSetOf(*_attrs)
    }

    internal constructor(_attrs: Set<Attr>) {
      this.attrs = persistentHashSetOf(*_attrs.toTypedArray())
    }

    override fun appendTo(printer: Printer) {
      if (attrs.isEmpty()) {
        return
      }

      for (a in attrs) {
        // Special handling to not add extra spaces before empty attributes
        if (a !is EmptyAttr) {
          printer.append(' ')
        }
        a.appendTo(printer)
      }
    }
  }

  private class AttrImpl(_key: String, _value: String) : Attr {
    override val key = _key
    override val value = _value

    override fun appendTo(printer: Printer) {
      printer.append(key)
          .append("=")
      printer.append('"')
      printer.append(StringEscapeUtils.escapeXml11(value))
      printer.append('"')
    }

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false
      other as Attr
      if (key != other.key) return false
      return true
    }

    override fun hashCode(): Int {
      return key.hashCode()
    }
  }

  private class MaskedNode(_node: Node, _mask: String) : Node {
    private val node = _node
    private val maskedXml = node(_mask)

    override fun appendTo(printer: Printer) {
      if (printer.shouldMask()) {
        maskedXml.appendTo(printer)
      } else {
        node.appendTo(printer)
      }
    }
  }

  private class MaskedText(_text: Text, _mask: String) : Text {
    private val text = _text
    private val maskedText = text(_mask)

    override fun appendTo(printer: Printer) {
      if (printer.shouldMask()) {
        maskedText.appendTo(printer)
      } else {
        text.appendTo(printer)
      }
    }
  }

  private class MaskedCdata(_cdata: Cdata, _mask: String) : Cdata {
    private val cdata = _cdata
    private val maskedCdata = cdata(_mask)

    override fun appendTo(printer: Printer) {
      if (printer.shouldMask()) {
        maskedCdata.appendTo(printer)
      } else {
        cdata.appendTo(printer)
      }
    }
  }

  private class MaskedAttr(_attr: Attr, _mask: String) : Attr {
    private val attr = _attr
    private val maskedAttr = attr(_attr.key, _mask)

    override val key: String
      get() = attr.key

    override val value: String
      get() = attr.value

    override fun appendTo(printer: Printer) {
      if (printer.shouldMask()) {
        maskedAttr.appendTo(printer)
      } else {
        attr.appendTo(printer)
      }
    }
  }

  /**
   * An interface for the Printer that serializes the XML
   * The default implementation is a StringBuilder Printer that outputs the StringBuilder contents
   * but a implementation could append the Xml to a file.
   */
  interface Printer : Appendable {
    val mask: Boolean

    fun shouldMask() = mask
  }

  /**
   * A Printer allows the printing of the XML to an appendable
   * This default printer can be used which appends to a StringBuilder.
   * An implementation could be supplied that appends to any Appendable class
   */
  class StringBuilderPrinter(_mask: Boolean = false) : Printer {
    private val appendable = StringBuilder()
    override val mask = _mask

    override fun append(csq: CharSequence?): Appendable {
      appendable.append(csq)
      return this
    }

    override fun append(csq: CharSequence?, p1: Int, p2: Int): Appendable {
      appendable.append(csq)
      return this
    }

    override fun append(c: Char): Appendable {
      appendable.append(c)
      return this
    }

    override fun toString() = appendable.toString()
  }

}
