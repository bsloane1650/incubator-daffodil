package edu.illinois.ncsa.daffodil.processors.unparsers

import org.junit.Test
import org.junit.Assert._
import edu.illinois.ncsa.daffodil.xml.XMLUtils
import edu.illinois.ncsa.daffodil.Implicits._
import edu.illinois.ncsa.daffodil.util.SchemaUtils
import edu.illinois.ncsa.daffodil.xml._
import edu.illinois.ncsa.daffodil.util.TestUtils
import edu.illinois.ncsa.daffodil.dsom.DiagnosticUtils

/*
 * These are all tests of OVC and alignment interactions
 */
class TestOutputValueCalcAndAlignment {

  val xsd = XMLUtils.XSD_NAMESPACE
  val dfdl = XMLUtils.dfdlAppinfoSource // XMLUtils.DFDL_NAMESPACE
  val xsi = XMLUtils.XSI_NAMESPACE
  val example = XMLUtils.EXAMPLE_NAMESPACE

  /**
   * Test verifies that if the OVC's length is known, the alignment afterwards
   * works.
   */
  @Test def testOutputValueCalcAlignmentFixed() {
    val sch = SchemaUtils.dfdlTestSchema(
      <dfdl:format ref="tns:daffodilTest1" encoding="ascii" lengthUnits="bytes" alignmentUnits="bytes" fillByte="X"/>,
      <xs:element name="e1" dfdl:lengthKind="implicit">
        <xs:complexType>
          <xs:sequence>
            <xs:element name="s1" type="xs:string" dfdl:lengthKind="explicit" dfdl:length="1" dfdl:outputValueCalc="{ ../s2 }"/>
            <xs:element name="s2" type="xs:string" dfdl:lengthKind="explicit" dfdl:length="1" dfdl:alignment="2"/>
          </xs:sequence>
        </xs:complexType>
      </xs:element>, elementFormDefault = "unqualified")
    val infoset = <ex:e1 xmlns:ex={ example }><s2>2</s2></ex:e1>
    val areTracing = false
    TestUtils.testUnparsing(sch, infoset, "2X2", areTracing)
  }

  /**
   * Tests that alignment will cause a suspended alignment region, until
   * the variable-length of the preceding OVC element is determined, at which
   * point then the absolute position is known, the alignment can proceed,
   * and the unparsing will complete.
   */
  @Test def testOutputValueCalcVariableLengthThenAlignment() {
    val sch = SchemaUtils.dfdlTestSchema(
      <dfdl:format ref="tns:daffodilTest1" encoding="ascii" lengthUnits="bytes" alignmentUnits="bytes" fillByte="X"/>,
      <xs:element name="e1" dfdl:lengthKind="implicit">
        <xs:complexType>
          <xs:sequence>
            <!-- 
             corner case: OVC element has variable length. So aligned element 
             after it cannot be unparsed (alignment region size isn't known) 
             until we know just how big the representation of s1 is, in bytes. 
             So the alignment must be deferred. 
             -->
            <xs:element name="s1" type="xs:string" dfdl:lengthKind="delimited" dfdl:terminator="T" dfdl:outputValueCalc="{ ../s2 }"/>
            <xs:element name="s2" type="xs:string" dfdl:lengthKind="delimited" dfdl:terminator="S" dfdl:alignment="8"/>
          </xs:sequence>
        </xs:complexType>
      </xs:element>, elementFormDefault = "unqualified")
    val infoset = <ex:e1 xmlns:ex={ example }><s2>12345</s2></ex:e1>
    val areTracing = false
    //
    // below, the two XX are inserted to align to an 8 byte boundary. But the
    // number of bytes to insert to achieve alignment can't be computed until
    // the OVC element gets its value, and therefore it can be unparsed, which
    // finishes the suspended unparse for the OVC, the absolute position 
    // propagates to the next (buffered) data output stream, so when the
    // alignment is retried, then it can determine 2 bytes are needed.
    //
    TestUtils.testUnparsing(sch, infoset, "12345TXX12345S", areTracing)
  }

  /**
   * Tests that alignment region will cause deadlock if preceding OVC is
   * variable length (such that the absolute position can't be known.
   *
   * This is the outputValueCalc depends on length which depends on interior alignment issue.
   *
   * This is supposed to deadlock.
   */
  @Test def testOutputValueCalcVariableLengthThenAlignmentDeadlock() {
    val sch = SchemaUtils.dfdlTestSchema(
      <dfdl:format ref="tns:daffodilTest1" encoding="ascii" lengthUnits="bytes" alignmentUnits="bytes" fillByte="X"/>,
      <xs:element name="e1" dfdl:lengthKind="implicit">
        <xs:complexType>
          <xs:sequence>
            <!-- 
             corner case: OVC element has variable length. 
             So alignment needed for i2 can't be determined since we don't have
             the starting absolute position.
             However, that alignment region length is needed to compute
             the dfdl:contentLength of s2. So we deadlock. 
             -->
            <xs:element name="s1" type="xs:string" dfdl:lengthKind="delimited" dfdl:terminator="T" dfdl:outputValueCalc="{ dfdl:contentLength(../s2, 'bytes') }"/>
            <xs:element name="s2" dfdl:lengthKind="delimited" dfdl:terminator="S">
              <xs:complexType>
                <xs:sequence>
                  <xs:element name="i1" type="xs:string" dfdl:lengthKind="explicit" dfdl:length="1"/>
                  <xs:element name="i2" type="xs:string" dfdl:lengthKind="explicit" dfdl:length="1" dfdl:alignment="8"/>
                </xs:sequence>
              </xs:complexType>
            </xs:element>
          </xs:sequence>
        </xs:complexType>
      </xs:element>, elementFormDefault = "unqualified")
    val infoset = <ex:e1 xmlns:ex={ example }><s2><i1>A</i1><i2>B</i2></s2></ex:e1>
    val areTracing = false
    val e = intercept[Exception] {
      TestUtils.testUnparsing(sch, infoset, "ignored", areTracing)
    }
    val msg = DiagnosticUtils.getSomeMessage(e).get.toLowerCase
    assertTrue(msg.contains("Schema Definition Error".toLowerCase))
    assertTrue(msg.contains("Deadlock".toLowerCase))
  }

}
