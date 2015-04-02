/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dataArtisans.flinkCascading.exec.operators;

import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.util.TupleBuilder;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.java.tuple.Tuple3;

public class ProjectionMapper extends RichMapFunction<Tuple, Tuple> {

	private Fields inputFields;
	private Fields outputFields;

	private transient TupleBuilder outputBuilder;

	public ProjectionMapper() {}

	public ProjectionMapper(Fields inputFields, Fields outputFields) {
		this.inputFields = inputFields;
		this.outputFields = outputFields;
	}

	@Override
	public void open(org.apache.flink.configuration.Configuration parameters) throws Exception {

		this.outputBuilder = new TupleBuilder() {

			@Override
			public Tuple makeResult(Tuple input, Tuple output) {
				return input.get( inputFields, outputFields );
			}
		};
	}

	@Override
	public Tuple map(Tuple inT) throws Exception {

		return this.outputBuilder.makeResult(inT, null);
	}

}
