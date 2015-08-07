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

package com.dataArtisans.flinkCascading.exec.hashJoin;

import cascading.tuple.Tuple;
import org.apache.flink.api.common.functions.MapPartitionFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.util.Collector;

public class JoinPrepareMapper implements MapPartitionFunction<Tuple, Tuple2<Tuple, Tuple[]>> {

	private Tuple2<Tuple, Tuple[]> out;

	public JoinPrepareMapper(int numJoins) {
		Tuple[] tupleList = new Tuple[numJoins-1];
		for(int i=0; i<numJoins-1; i++) {
			tupleList[i] = new Tuple();
		}
		out = new Tuple2<Tuple, Tuple[]>(null, tupleList);
	}

	@Override
	public void mapPartition(Iterable<Tuple> tuples, Collector<Tuple2<Tuple, Tuple[]>> collector) throws Exception {
		for(Tuple t : tuples) {
			out.f0 = t;
			collector.collect(out);
		}
	}

}