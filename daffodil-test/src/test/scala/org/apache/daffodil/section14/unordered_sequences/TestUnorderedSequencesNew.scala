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

package org.apache.daffodil.section14.unordered_sequences

import org.apache.daffodil.util.Misc
import org.apache.daffodil.tdml.DFDLTestSuite

class TestUnorderedSequencesNew {
  val testDir = "/org/apache/daffodil/section14/unordered_sequences/"
  val uos = testDir + "UnorderedSequences.tdml"
  val res = Misc.getRequiredResource(uos)
  lazy val runner = new DFDLTestSuite(res)

  //DFDL-1010
  //@Test def test_simple = { runner.runOneTest("test_simple") }
  //@Test def test_simple_fail_scalar = { runner.runOneTest("test_simple_fail_scalar") }
  //@Test def test_simple_min_max_occurs = { runner.runOneTest("test_simple_min_max_occurs") }
  //@Test def test_simple_min_max_occurs_fail = { runner.runOneTest("test_simple_min_max_occurs_fail") }
  //@Test def test_simple_delimited = { runner.runOneTest("test_simple_delimited") }
  //@Test def test_simple_nil = { runner.runOneTest("test_simple_nil") }
  //@Test def test_simple_invalid_path_to_branch = { runner.runOneTest("test_simple_invalid_path_to_branch") }
  //@Test def test_simple_invalid_path_to_branch_does_not_exist = { runner.runOneTest("test_simple_invalid_path_to_branch_does_not_exist") }
  //@Test def test_nested_valid_path_to_branch = { runner.runOneTest("test_nested_valid_path_to_branch") }
  //@Test def test_nested_multiple_valid_paths_to_branch = { runner.runOneTest("test_nested_multiple_valid_paths_to_branch") }
  //@Test def test_nested_multiple_invalid_paths_to_branch = { runner.runOneTest("test_nested_multiple_invalid_paths_to_branch") }
  //@Test def test_nested_path_evaluates_to_nodelist = { runner.runOneTest("test_nested_path_evaluates_to_nodelist") }
  //@Test def test_nested_invalid_path_to_branch = { runner.runOneTest("test_nested_invalid_path_to_branch") }
  //@Test def test_nested_invalid_path_to_branch_2 = { runner.runOneTest("test_nested_invalid_path_to_branch_2") }
  //@Test def test_nested_invalid_path_to_branch_3 = { runner.runOneTest("test_nested_invalid_path_to_branch_3") }
  //@Test def test_sde_element_element_ref = { runner.runOneTest("test_sde_element_element_ref") }
  //@Test def test_sde_optional_array_ock_parsed = { runner.runOneTest("test_sde_optional_array_ock_parsed") }
  //@Test def test_sde_unique_names_in_ns = { runner.runOneTest("test_sde_unique_names_in_ns") }

}
