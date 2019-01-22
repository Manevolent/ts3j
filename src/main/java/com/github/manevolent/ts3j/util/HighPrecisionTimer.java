/*
 *     This file is part of Discord4J.
 *
 *     Discord4J is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Discord4J is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with Discord4J.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.manevolent.ts3j.util;

import java.util.concurrent.locks.LockSupport;

/**
 * Custom Thread that executes the passed task every specified period.
 *
 * Time of execution is held in account when calculating the time to sleep until the next execution.
 */
public class HighPrecisionTimer extends Thread {
	private final int periodInNanos;
	private final int maximumDriftInNanos;
	private final Runnable target;
	private volatile boolean stop;

	/**
	 * @param periodInMilis The period in milliseconds of the task execution.
	 * @param catchupTicks The number of ticks allowed to consecutively catch up to
	 * @param target The task to run.
	 */
	public HighPrecisionTimer(int periodInMilis,
							  float catchupTicks,
							  Runnable target) {
		super("TS3J AudioThread");

		this.periodInNanos = periodInMilis * 1_000_000;
		this.maximumDriftInNanos = (int) Math.floor((double)periodInNanos * (double)catchupTicks);
		this.target = target;
	}

	public void gracefulStop() {
		if (!this.stop) {
			this.stop = true;
			//if (isAlive()) interrupt();
		}
	}

	@Override
	@SuppressWarnings(value = "empty-statement")
	public void run() {
		long now = System.nanoTime();
		long nextTarget = now;

		while (!stop) {
			target.run();

			nextTarget += periodInNanos;

			now = System.nanoTime();

			// Clamp the next target to the maximum "drift" allowed
			if (nextTarget - now > maximumDriftInNanos)
				nextTarget = now - maximumDriftInNanos;

			if (sleepFor(nextTarget - now)) {
				while (nextTarget > System.nanoTime()) {
					; //consume cycles
				}
			}
		}
	}

	private boolean sleepFor(long nanos) {
		if (nanos > 0) {
			long elapsed = 0;
			while (elapsed < nanos) {
				long t0 = System.nanoTime();
				LockSupport.parkNanos(nanos - elapsed);
				elapsed += System.nanoTime() - t0;
			}

			return true;
		}

		return false;
	}

}