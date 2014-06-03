package edu.illinois.ncsa.daffodil.dsom

/* Copyright (c) 2012-2013 Tresys Technology, LLC. All rights reserved.
 *
 * Developed by: Tresys Technology, LLC
 *               http://www.tresys.com
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal with
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimers.
 * 
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimers in the
 *     documentation and/or other materials provided with the distribution.
 * 
 *  3. Neither the names of Tresys Technology, nor the names of its contributors
 *     may be used to endorse or promote products derived from this Software
 *     without specific prior written permission.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * CONTRIBUTORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS WITH THE
 * SOFTWARE.
 */

import scala.xml.Node

abstract class ComplexTypeBase(xmlArg: Node, parent: SchemaComponent)
  extends SchemaComponent(xmlArg, parent)
  with TypeBase
  with ComplexTypeBaseGrammarMixin {

  requiredEvaluations(modelGroup)

  def element: ElementBase

  lazy val <complexType>{ xmlChildren @ _* }</complexType> = xml

  lazy val Seq(modelGroup) = {
    val s = smg.value
    // TODO: why check this? Schema validation will enforce this for us. (I think).
    schemaDefinitionUnless(s.length == 1, "A complex type must have exactly one model-group element child which is a sequence, choice, or group reference.")
    s
  }

  private val smg = LV('smg) {
    xmlChildren.flatMap {
      xmlChild =>
        {
          val g = GroupFactory(xmlChild, this, 1) // discards unwanted text nodes also.
          g
        }
    }
  }

  // provides needed polymorphism across unannotated complex types, and
  // the annotated objects.
  lazy val localAndFormatRefProperties: Map[String, String] = {
    Map.empty[String, String]
  }

  lazy val isScannable: Boolean = {
    val parentElem: ElementBase = enclosingComponent.get.asInstanceOf[ElementBase]
    modelGroup.group.isScannable
    val unScannableChildren = modelGroup.group.groupMembers.filterNot { child =>
      (child.knownEncodingCharset == parentElem.knownEncodingCharset) && child.isScannable
    }
    unScannableChildren.length == 0
  }

  lazy val alignmentValueInBits: Int = {
    val children = modelGroup.group.groupMembers.sortBy(m => -m.alignmentValueInBits)
    children.headOption match {
      case Some(child) => child.alignmentValueInBits
      case None => 0
    }
  }
}

class GlobalComplexTypeDefFactory(xmlArg: Node, schemaDocumentArg: SchemaDocument)
  extends SchemaComponent(xmlArg, schemaDocumentArg) with NamedMixin {

  def forElement(element: ElementBase) = new GlobalComplexTypeDef(xml, schemaDocument, element)

}

class GlobalComplexTypeDef(xmlArg: Node, schemaDocumentArg: SchemaDocument, val element: ElementBase)
  extends ComplexTypeBase(xmlArg, schemaDocumentArg)
  with GlobalComponentMixin {

  lazy val referringComponent = Some(element)

}

class LocalComplexTypeDef(xmlArg: Node, val element: ElementBase)
  extends ComplexTypeBase(xmlArg, element)
  with LocalComponentMixin {
  //nothing
}