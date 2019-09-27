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

import com.google.common.util.concurrent.AtomicDouble;
import com.netflix.control.IActuator;
import com.netflix.control.controllers.ControlLoop;
import io.vavr.Tuple;
import org.junit.Test;
import rx.Observable;
import rx.observers.TestSubscriber;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;


public class ControlLoopTest {

    @Test public void shouldRemainInSteadyState() {
        ClutchConfiguration config = ClutchConfiguration.builder()
                .cooldownInterval(10L)
                .cooldownUnits(TimeUnit.MILLISECONDS)
                .metric(Clutch.Metric.CPU)
                .kd(0.01)
                .kp(0.01)
                .kd(0.01)
                .maxSize(10)
                .minSize(3)
                .rope(Tuple.of(0.25, 0.0))
                .setPoint(0.6)
                .build();

        TestSubscriber<Double> subscriber = new TestSubscriber<>();

        Observable.range(0, 1000)
                .map(__ -> new Event(Clutch.Metric.CPU, 0.5))
                .compose(new ControlLoop(config, IActuator.of(x -> x), 8.0))
                .toBlocking()
                .subscribe(subscriber);

        subscriber.assertNoErrors();
        subscriber.assertCompleted();
        assertThat(subscriber.getOnNextEvents()).allSatisfy(x -> assertThat(x).isEqualTo(8.0));
    }

        @Test public void shouldBeUnperturbedByOtherMetrics() {
            ClutchConfiguration config = ClutchConfiguration.builder()
                    .cooldownInterval(10L)
                    .cooldownUnits(TimeUnit.MILLISECONDS)
                    .metric(Clutch.Metric.CPU)
                    .kd(0.01)
                    .kp(0.01)
                    .kd(0.01)
                    .maxSize(10)
                    .minSize(3)
                    .rope(Tuple.of(0.25, 0.0))
                    .setPoint(0.6)
                    .build();

            TestSubscriber<Double> subscriber = new TestSubscriber<>();

            Observable<Event> cpu = Observable.range(0, 1000)
                    .map(__ -> new Event(Clutch.Metric.CPU, 0.5));

            Observable<Event> network = Observable.range(0, 1000)
                    .map(__ -> new Event(Clutch.Metric.NETWORK, 0.1));

            cpu.mergeWith(network)
                    .compose(new ControlLoop(config, IActuator.of(x -> x), 8.0))
                    .toBlocking()
                    .subscribe(subscriber);

            subscriber.assertNoErrors();
            subscriber.assertCompleted();
            assertThat(subscriber.getOnNextEvents()).allSatisfy(x -> assertThat(x).isEqualTo(8.0));
    }

    @Test public void shouldScaleUp() {
        ClutchConfiguration config = ClutchConfiguration.builder()
                .cooldownInterval(10L)
                .cooldownUnits(TimeUnit.MILLISECONDS)
                .metric(Clutch.Metric.CPU)
                .kd(0.01)
                .kp(0.01)
                .kd(0.01)
                .maxSize(10)
                .minSize(3)
                .rope(Tuple.of(0.25, 0.0))
                .setPoint(0.6)
                .build();

        TestSubscriber<Double> subscriber = new TestSubscriber<>();

        Observable.range(0, 1000)
                .map(__ -> new Event(Clutch.Metric.CPU, 0.7))
                .compose(new ControlLoop(config, IActuator.of(Math::ceil), 8.0))
                .toBlocking()
                .subscribe(subscriber);

        subscriber.assertNoErrors();
        subscriber.assertCompleted();
        assertThat(subscriber.getOnNextEvents()).allSatisfy(x -> assertThat(x).isEqualTo(9.0));
    }

    @Test public void shouldScaleDown() {
        ClutchConfiguration config = ClutchConfiguration.builder()
                .cooldownInterval(10L)
                .cooldownUnits(TimeUnit.MILLISECONDS)
                .metric(Clutch.Metric.CPU)
                .kd(0.01)
                .kp(0.01)
                .kd(0.01)
                .maxSize(10)
                .minSize(3)
                .rope(Tuple.of(0.25, 0.0))
                .setPoint(0.6)
                .build();

        TestSubscriber<Double> subscriber = new TestSubscriber<>();

        Observable.range(0, 1000)
                .map(__ -> new Event(Clutch.Metric.CPU, 0.2))
                .compose(new ControlLoop(config, IActuator.of(Math::ceil), 8.0))
                .toBlocking()
                .subscribe(subscriber);

        subscriber.assertNoErrors();
        subscriber.assertCompleted();
        assertThat(subscriber.getOnNextEvents()).allSatisfy(x -> assertThat(x).isLessThan(8.0));
    }

    @Test public void shouldScaleUpAndDown() {
        ClutchConfiguration config = ClutchConfiguration.builder()
                .cooldownInterval(0L)
                .cooldownUnits(TimeUnit.MILLISECONDS)
                .metric(Clutch.Metric.CPU)
                .kd(0.1)
                .kp(0.5)
                .kd(0.1)
                .maxSize(10)
                .minSize(3)
                .rope(Tuple.of(0.0, 0.0))
                .setPoint(0.6)
                .build();

        TestSubscriber<Double> subscriber = new TestSubscriber<>();

        Observable.range(0, 1000)
                .map(tick -> new Event(Clutch.Metric.CPU, ((tick % 60.0) + 30.0) / 100.0))
                .compose(new ControlLoop(config, IActuator.of(Math::ceil), 5.0))
                .toBlocking()
                .subscribe(subscriber);

        subscriber.assertNoErrors();
        subscriber.assertCompleted();
        assertThat(subscriber.getOnNextEvents()).anySatisfy(x -> assertThat(x).isLessThan(5.0));
        assertThat(subscriber.getOnNextEvents()).anySatisfy(x -> assertThat(x).isGreaterThan(5.0));
    }
}