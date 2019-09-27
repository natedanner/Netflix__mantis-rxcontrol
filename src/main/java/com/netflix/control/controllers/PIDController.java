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

package com.netflix.control.controllers;

import com.google.common.util.concurrent.AtomicDouble;
import com.netflix.control.IController;

/**
 * The Feedback Principle: Constantly compare the actual output to the 
 * setpoint; then apply a corrective action in the proper direction and
 * approximately of the correct size.
 *
 * Iteratively applying changes in the correct direction allows this
 * system to converge onto the correct value over time.
 *
 */
public class PIDController extends IController {

  private final Double kp; // Proportional Gain
  private final Double ki; // Integral Gain
  private final Double kd; // Derivative Gain
  private Double previous = 0.0;

  private final double deltaT;
  private final AtomicDouble dampener;

  private Double integral = 0.0;
  private Double derivative = 0.0;

  /**
   * Implements a Proportional-Integral-Derivative (PID) three term control
   * system.
   *
   * @param kp The gain for the proportional component of the controller.
   * @param ki The gain for the integral component of the controller.
   * @param kd The gain for the derivative component of the controller.
   * @param deltaT The time delta. A useful default is 1.0.
   * @param dampener A dampening signal which can be used for gain scheduling.
   *
   * Setting the gain for an individual component disables said
   * component. For example setting kd to 0.0 creates a PI (two term) control
   * system.
   *
   * Gain scheduling is a method of manipulating the behavior of a PID
   * controller at runtime. The concept is that different gain schedules might
   * be appropriate at different times. Some examples;
   *
   * Oscillation: High gain can exacerbate and even cause oscillation.
   * Chaos Kong: Increasing gain to accelerate scale ups.
   */
  public PIDController(Double kp, Double ki, Double kd, Double deltaT, AtomicDouble dampener) {
    this.kp = kp;
    this.ki = ki;
    this.kd = kd;
    this.deltaT = deltaT;
    this.dampener = dampener;
  }

  public PIDController(Double kp, Double ki, Double kd, Double deltaT) {
    this(kp, ki, kd, deltaT, new AtomicDouble(1.0));
  }

  @Override
  public Double processStep(Double error) {
        this.integral += this.deltaT * error;
        this.derivative =  (error - this.previous) / this.deltaT;
        this.previous = error;

        double d = this.dampener.get();

        return this.kp * d * error
                + this.ki * d * this.integral
                + this.kd * d * this.derivative;
  }

  public static PIDController of(Double kp, Double ki, Double kd, AtomicDouble dampener) {
    return new PIDController(kp, ki, kd, 1.0, dampener);
  }

  public static PIDController of(Double kp, Double ki, Double kd, Double deltaT) {
    return new PIDController(kp, ki, kd, deltaT);
  }

  public static PIDController of(Double kp, Double ki, Double kd) {
    return new PIDController(kp, ki, kd, 1.0, new AtomicDouble(1.0));
  }

  public AtomicDouble getDampener() {
    return this.dampener;
  }
}