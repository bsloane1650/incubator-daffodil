/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.daffodil.processors.unparsers

import org.apache.daffodil.dsom.CompiledExpression
import org.apache.daffodil.processors.NonTermRuntimeData
import org.apache.daffodil.processors.VariableRuntimeData
import org.apache.daffodil.dpath.SuspendableExpression
import org.apache.daffodil.util.MaybeULong
import org.apache.daffodil.processors.RuntimeData
import org.apache.daffodil.processors.ElementRuntimeData
import org.apache.daffodil.processors.Evaluatable
import org.apache.daffodil.exceptions.Assert
import org.apache.daffodil.processors.TypeCalculator
import org.apache.daffodil.util.Maybe
import org.apache.daffodil.processors.Success
import org.apache.daffodil.infoset.Infoset
import org.apache.daffodil.infoset.DISimple
import org.apache.daffodil.infoset.EndElement

final class SetVariableSuspendableExpression(
  override val expr: CompiledExpression[AnyRef],
  override val rd: VariableRuntimeData,
  referencingContext: NonTermRuntimeData)
  extends SuspendableExpression {

  override protected def processExpressionResult(ustate: UState, v: AnyRef) {
    val newVMap =
      ustate.variableMap.setVariable(rd, v, referencingContext, ustate)

    ustate.setVariables(newVMap)
  }

  override protected def maybeKnownLengthInBits(ustate: UState) = MaybeULong(0)
}

/**
 * Used when unparsing to evaluate dfdl:setVariable statements.
 *
 * TODO: Possible bug. This will allow expressions to forward reference, even
 * when the variables are being referenced from expressions that are NOT
 * allowed to forward reference - e.g., property value expressions such
 * as delimiters and byte order.
 *
 * This forward suspension is only supposed to be allowed for dfdl:outputValueCalc.
 */
final class SetVariableUnparser(
  val expr: CompiledExpression[AnyRef],
  override val context: VariableRuntimeData,
  referencingContext: NonTermRuntimeData)
  extends PrimUnparserNoData {

  override lazy val runtimeDependencies = Vector()

  override lazy val childProcessors = Vector()

  def suspendableExpression =
    new SetVariableSuspendableExpression(
      expr, context, referencingContext)

  override def unparse(state: UState): Unit = {
    suspendableExpression.run(state)
  }

}

// When implemented this almost certainly wants to be a combinator
// Not two separate unparsers.
class NewVariableInstanceStartUnparser(override val context: RuntimeData)
  extends PrimUnparserNoData {

  override lazy val runtimeDependencies = Vector()

  override lazy val childProcessors = Vector()

  context.notYetImplemented("newVariableInstance")

  override def unparse(ustate: UState) = {
    context.notYetImplemented("newVariableInstance")
  }
}

class NewVariableInstanceEndUnparser(override val context: RuntimeData)
  extends PrimUnparserNoData {

  override lazy val runtimeDependencies = Vector()

  override lazy val childProcessors = Vector()

  context.notYetImplemented("newVariableInstance")

  override def unparse(ustate: UState) = {
    context.notYetImplemented("newVariableInstance")
  }
}

class TypeValueCalcUnparser(typeCalculator: TypeCalculator[AnyRef, AnyRef], repTypeUnparser: Unparser, e: ElementRuntimeData, repTypeRuntimeData: ElementRuntimeData)
  extends CombinatorUnparser(e) {

  override def childProcessors: Vector[Unparser] = Vector(repTypeUnparser)

  def runtimeDependencies: Vector[Evaluatable[AnyRef]] = Vector()

  protected def unparse(ustate: UState): Unit = {
    Assert.invariant(ustate.currentInfosetNodeMaybe.isDefined)
    Assert.invariant(ustate.currentInfosetNode.isSimple)

    val currentSimple = ustate.currentInfosetNode.asSimple
    
    val logicalValue: AnyRef = currentSimple.dataValue
    val logicalValueType = currentSimple.erd.optPrimType.get
    val repTypeValue: Maybe[AnyRef] = typeCalculator.outputTypeCalcUnparse(ustate, e, logicalValue, logicalValueType)

    val origInfosetElement = ustate.currentInfosetNode
    val tmpInfosetElement = Infoset.newElement(repTypeRuntimeData, repTypeRuntimeData.tunable).asInstanceOf[DISimple]

    if (ustate.processorStatus == Success) {

      Assert.invariant(repTypeValue.isDefined)
      tmpInfosetElement.setDataValue(repTypeValue.get)
      ustate.currentInfosetNodeStack.push(Maybe(tmpInfosetElement))

      try {
        repTypeUnparser.unparse1(ustate)
      } catch {
        case e: Throwable => {
          e.printStackTrace()
        }
      } finally {
        ustate.currentInfosetNodeStack.pop
      }
    }

  }

}
