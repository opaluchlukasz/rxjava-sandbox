package com.github.opaluchlukasz.rxjavasandbox;

import io.reactivex.Flowable;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.Scanner;

/**
 * Example implementation of rss feed publisher and subscriber using backpressure mechanism.
 */
public class RssFeed {
    private static final String[] RSS_FEED = {
            "Writing code that nobody else can read",
            "Blaming the architecture",
            "Z-Index: 10000000",
            "RegExp by trial and error",
            "Solving imaginary scaling issues",
            "Getting an Arduino LED to blink",
            "Blaming the user"
    };

    public static void main(String[] args) {
        final Flowable<String> newsFeed = Flowable.fromPublisher(subscriber -> subscriber.onSubscribe(new Subscription() {
            private int alreadyPublished = 0;

            @Override
            public void request(long requested) {
                int remainingArticles = RSS_FEED.length - alreadyPublished;
                if (remainingArticles > 0) {
                    while(requested > 0) {
                        requested--;

                        subscriber.onNext(RSS_FEED[alreadyPublished]);
                        alreadyPublished++;

                        if(alreadyPublished == RSS_FEED.length) {
                            subscriber.onComplete();
                            break;
                        }
                    }
                }
            }

            @Override
            public void cancel() {
                subscriber.onComplete();
            }
        }));

        newsFeed.blockingSubscribe(new Subscriber<String>() {
            private Scanner in = new Scanner(System.in);
            private Subscription subscription;
            private int totalRequested = 0;
            private int totalReceived = 0;

            @Override
            public void onSubscribe(Subscription subscription) {
                this.subscription = subscription;
                requestNextBatch();
            }

            @Override
            public void onNext(String s) {
                System.out.println(s);
                totalReceived++;

                if(totalRequested == totalReceived) {
                    requestNextBatch();
                }
            }

            @Override
            public void onError(Throwable throwable) {
                // NOOP
            }

            @Override
            public void onComplete() {
                System.out.println("-----------");
            }

            private void requestNextBatch() {
                int requested = in.nextInt();
                totalRequested += requested;
                subscription.request(requested);
            }
        });
    }
}
