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
package org.apache.hadoop.hive.schshim;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.shims.SchedulerShim;

public class FairSchedulerShim implements SchedulerShim {
  private static final Logger LOG = LoggerFactory.getLogger(FairSchedulerShim.class);
  private static final String MR2_JOB_QUEUE_PROPERTY = "mapreduce.job.queuename";

  @Override
  public void refreshDefaultQueue(Configuration conf, String userName)
      throws IOException {
    // In Hadoop 3.4, the FairScheduler internal APIs (AllocationFileLoaderService,
    // AllocationConfiguration, QueuePlacementPolicy) were refactored to require a
    // live FairScheduler instance and QueuePlacementPolicy was made package-private.
    // These classes can no longer be used standalone from outside the
    // org.apache.hadoop.yarn.server.resourcemanager.scheduler.fair package.
    //
    // Queue placement for the FairScheduler is now handled internally by the
    // ResourceManager's PlacementManager. When HiveServer2 runs in non-impersonation
    // mode, the queue mapping should be configured on the ResourceManager side
    // (e.g., via fair-scheduler.xml placement rules) rather than resolved client-side.
    LOG.debug("FairScheduler queue placement is managed by the ResourceManager's " +
        "PlacementManager in Hadoop 3.4+. Skipping client-side queue resolution " +
        "for user: {}", userName);
  }

}
