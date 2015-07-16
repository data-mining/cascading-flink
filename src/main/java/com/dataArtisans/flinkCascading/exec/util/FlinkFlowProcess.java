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

package com.dataArtisans.flinkCascading.exec.util;

import cascading.CascadingException;
import cascading.flow.FlowProcess;
import cascading.flow.FlowSession;
import cascading.flow.hadoop.HadoopFlowProcess;
import cascading.flow.hadoop.util.HadoopUtil;
import cascading.tap.Tap;
import cascading.tap.hadoop.Hfs;
import cascading.tuple.TupleEntryCollector;
import cascading.tuple.TupleEntryIterator;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.util.InstantiationUtil;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.JobConf;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FlinkFlowProcess extends FlowProcess<Configuration> {

	private transient RuntimeContext runtimeContext;
	private Configuration conf;
	private String taskId;

	public FlinkFlowProcess() {}

	public FlinkFlowProcess(Configuration conf) {
		this.conf = conf;
	}

	public FlinkFlowProcess(FlowSession flowSession, Configuration conf) {
		super(flowSession);
		this.conf = conf;
	}

	public FlinkFlowProcess(Configuration conf, RuntimeContext runtimeContext, String taskId) {

		this(conf);
		this.runtimeContext = runtimeContext;
		this.taskId = taskId;
	}

	@Override
	public int getNumProcessSlices() {
		return this.runtimeContext.getNumberOfParallelSubtasks();
	}

	@Override
	public int getCurrentSliceNum() {
		return this.runtimeContext.getIndexOfThisSubtask();
	}

	@Override
	public FlowProcess copyWith(Configuration config) {
		return new FlinkFlowProcess(config, this.runtimeContext, this.taskId);
	}

	@Override
	public Object getProperty( String key ) {
		return this.conf.get(key);
	}

	@Override
	public Collection<String> getPropertyKeys() {
		Set<String> keys = new HashSet<String>();

		for( Map.Entry<String, String> entry : this.conf ) {
			keys.add(entry.getKey());
		}

		return Collections.unmodifiableSet( keys );
	}

	@Override
	public Object newInstance(String className) {
		if(className == null || className.isEmpty()) {
			return null;
		}

		try {
			Class clazz = Class.forName(className);
			return InstantiationUtil.instantiate(clazz);
		}
		catch( ClassNotFoundException exception ) {
			throw new CascadingException( "unable to load class: " + className.toString(), exception );
		}
	}

	@Override
	public void keepAlive() {
		// Hadoop reports progress
		// Tez doesn't do anything here either...
	}

	@Override
	public void increment(Enum e, long l) {
//		throw new UnsupportedOperationException("Enum counters not supported."); // TODO
	}

	@Override
	public void increment(String group, String counter, long l) {
		if(this.runtimeContext == null) {
			throw new RuntimeException("RuntimeContext has not been set.");
		}
		else {
			this.runtimeContext.getLongCounter(group+"."+counter).add(l);
		}
	}

	@Override
	public long getCounterValue(Enum anEnum) {
//		throw new UnsupportedOperationException("Enum counters not supported."); // TODO
		return -1l;
	}

	@Override
	public long getCounterValue(String group, String counter) {
		if(this.runtimeContext == null) {
			throw new RuntimeException("RuntimeContext has not been set.");
		}
		else {
			return this.runtimeContext.getLongCounter(group+"."+counter).getLocalValue();
		}
	}

	@Override
	public void setStatus(String s) {
		// Tez doesn't do anything here either
	}

	@Override
	public boolean isCounterStatusInitialized() {
		return this.runtimeContext != null;
	}

	@Override
	public TupleEntryIterator openTapForRead(Tap tap) throws IOException {
		return tap.openForRead( this );
	}

	@Override
	public TupleEntryCollector openTapForWrite(Tap tap) throws IOException {
		return tap.openForWrite( this, null ); // do not honor sinkmode as this may be opened across tasks
	}

	@Override
	public TupleEntryCollector openTrapForWrite(Tap trap) throws IOException {

		if (trap instanceof Hfs) {

			JobConf jobConf = new JobConf(this.getConfigCopy());

			String partname = String.format("-%s-%05d-", this.taskId, this.getCurrentSliceNum());
			jobConf.set( "cascading.tapcollector.partname", "%s%spart" + partname + "%05d" );

			return trap.openForWrite( new HadoopFlowProcess( jobConf ), null );
		}
		else {
			throw new UnsupportedOperationException("Only Hfs taps are supported as traps");
		}
	}

	@Override
	public TupleEntryCollector openSystemIntermediateForWrite() throws IOException {
		return null; // Not required for Flink
	}

	@Override
	public Configuration getConfig() {
		return conf;
	}

	@Override
	public Configuration getConfigCopy() {
		return HadoopUtil.copyJobConf(this.conf);
	}

	@Override
	public <C> C copyConfig(C conf) {
		return HadoopUtil.copyJobConf(conf);
	}

	@Override
	public <C> Map<String, String> diffConfigIntoMap(C defaultConfig, C updatedConfig) {

		Map<String, String> newConf = new HashMap<String, String>();
		for(Map.Entry<String,String> e : ((Configuration)updatedConfig)) {
			String key = e.getKey();
			String val = ((Configuration) updatedConfig).get(key);
			String defaultVal = ((Configuration)defaultConfig).get(key);

			// add keys that are different from default
			if((val == null && defaultVal == null) || val.equals(defaultVal)) {
				continue;
			}
			else {
				newConf.put(key, val);
			}
		}

		return newConf;
	}

	@Override
	public Configuration mergeMapIntoConfig(Configuration defaultConfig, Map<String, String> map) {

		Configuration mergedConf = HadoopUtil.copyJobConf(defaultConfig);
		for(String key : map.keySet()) {
			mergedConf.set(key, map.get(key));
		}
		return mergedConf;
	}

}
