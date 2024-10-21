package com.sapienter.jbilling.common;

import java.util.Arrays;
import java.util.Random;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;

@Aspect
public class ConcurrentOperationFailureInterceptor implements Ordered {
 
    private static final Logger logger = LoggerFactory.getLogger(ConcurrentOperationFailureInterceptor.class);
     
     private int maxRetries;
     public static final int PAUSE = 100;
     private static final Random rand = new Random();
     private int order;
     /**
         * Advice that traps an exception specified by an annotation so that the operation can be retried.
         *
         * @param pjp wrapper around method being executed
         * @param retryConcurrentOperation annotation indicating method should be wrapped
         * @return return value of wrapped call
         * @throws Exception if retries exceed maximum, rethrows exception configured in RetryConcurrentOperation annotation
         * @throws Throwable any other things the wrapped call throws will pass through
         */
     @Around("@annotation(retryConcurrentOperation)")
     public Object retry(ProceedingJoinPoint pjp, RetryConcurrentOperation retryConcurrentOperation) throws Throwable {
         int times = Math.min(retryConcurrentOperation.retries(), maxRetries);
         
         Class<? extends Throwable>[] retryOn = retryConcurrentOperation.on();
         Assert.isTrue(times > 0, "@RetryConcurrentOperation{retries} should be greater than 0!");
         Assert.isTrue(retryOn.length > 0, "@RetryConcurrentOperation{on} should have at least one Throwable!");
         logger.debug("Proceed with {} retries on {}", times, Arrays.toString(retryOn));
         return tryProceeding(pjp, times, retryOn);
     }

        private Object tryProceeding(ProceedingJoinPoint pjp, int times, Class<? extends Throwable>[] retryOn) throws Throwable {
            try {
                return pjp.proceed();
            } catch (Throwable throwable) {
                if (isRetryThrowable(throwable, retryOn) && times-- > 0) {
                    // Random has been used to offset pause time for concurrent threads executing in parallel
                    // so that they don't pause for the same exact amount of time.
                    pause(PAUSE + rand.nextInt(200));
                    logger.debug("Optimistic locking detected, {} remaining retries on {}", times, Arrays.toString(retryOn));
                    return tryProceeding(pjp, times, retryOn);
                }
                logger.debug("Login issue full stacktrace: {}", org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace(throwable));
                throw new SessionInternalError("Concurrent operation retries exausted.",throwable);
            }
        }

        private boolean isRetryThrowable(Throwable throwable, Class<? extends Throwable>[] retryOn) {
            Throwable[] causes = ExceptionUtils.getThrowables(throwable);
            for(Throwable cause : causes) {
                for(Class<? extends Throwable> retryThrowable : retryOn) {
                    if(retryThrowable.isAssignableFrom(cause.getClass())) {
                        return true;
                    }
                }
            }
            return false;
        }

        public Integer getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(Integer maxRetries) {
            this.maxRetries = maxRetries;
        }
        
        private void pause(long ms) {
            try {
               Thread.sleep(ms); 
            } catch(InterruptedException e) {
                // Do not swallow the InterruptedException by catching it and 
                // doing nothing in the catch block.
                // The standard way is to restore the interrupted status by calling interrupt again.
                Thread.currentThread().interrupt();
            }
        }

        //Return the order value of this object, in terms of sorting.
        @Override
        public int getOrder() {
            return this.order;
        }

		public void setOrder(int order) {
			this.order = order;
		}
    
}