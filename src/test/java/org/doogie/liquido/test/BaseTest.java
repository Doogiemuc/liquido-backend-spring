package org.doogie.liquido.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

@Slf4j
public class BaseTest {
	/**
	 * Entry and exit logging for <b>all</b> test cases. Jiipppiiee. Did I already mention that I am a logging fanatic *G*
	 */
	@Rule
	public TestWatcher slf4jTestWatcher = new TestWatcher() {
		@Override
		protected void starting(Description descr) {
			log.trace("===== TEST STARTING "+descr.getDisplayName());
		}

		@Override
		protected void failed(Throwable e, Description descr) {
			log.error("===== TEST FAILED "+descr.getDisplayName()+ ": "+e.toString());
		}

		@Override
		protected void succeeded(Description descr) {
			log.trace("===== TEST SUCCESS "+descr.getDisplayName());
		}
	};
}
