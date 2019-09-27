/*
 * Copyright 2019 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.control.clutch;

import java.util.concurrent.TimeUnit;

import io.vavr.Tuple2;
import lombok.Builder;
import lombok.Value;

/**
 * Represents the overall configuration of a Clutch control loop.
 */
@Builder @Value public class ClutchConfiguration {

    /** The Metric for which this configuration is intended. */
    public final Clutch.Metric metric;
    /** The setPoint will be the value for the metric tracked by the controller. */
    public final double setPoint;

    /** Proportional controller gain. */
    public final double kp;
    /** Integral controller gain. */
    public final double ki;
    /** Derivative controller gain. */
    public final double kd;

    /** Minimum size for autoscaling. */
    public final int minSize;
    /** Maximum size for autoscaling */
    public final int maxSize;

    /** Region of Practical Equivalence. Value below and above setPoint which is treated as equal to the setPoint. */
    public final Tuple2<Double, Double> rope;

    /** Cooldown interval for the autoscaler. */
    public final long cooldownInterval;
    /** Cooldown time units for the autoscaler. */
    public final TimeUnit cooldownUnits;
}
