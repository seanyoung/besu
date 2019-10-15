/*
 * Copyright ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.ethereum.api.jsonrpc.websocket.subscription.syncing;

import org.hyperledger.besu.ethereum.api.jsonrpc.internal.results.SyncingResult;
import org.hyperledger.besu.ethereum.api.jsonrpc.websocket.subscription.Subscription;
import org.hyperledger.besu.ethereum.api.jsonrpc.websocket.subscription.SubscriptionManager;
import org.hyperledger.besu.ethereum.api.jsonrpc.websocket.subscription.request.SubscriptionType;
import org.hyperledger.besu.ethereum.core.Synchronizer;
import org.hyperledger.besu.plugin.data.SyncStatus;

import java.util.Optional;

public class SyncingSubscriptionService {

  private final SubscriptionManager subscriptionManager;
  private Optional<Boolean> lastMessageWasInSync = Optional.empty();

  public SyncingSubscriptionService(
      final SubscriptionManager subscriptionManager, final Synchronizer synchronizer) {
    this.subscriptionManager = subscriptionManager;
    synchronizer.subscribeSyncStatus(this::sendSyncingToMatchingSubscriptions);
  }

  private void sendSyncingToMatchingSubscriptions(final SyncStatus syncStatus) {
    subscriptionManager.notifySubscribersOnWorkerThread(
        SubscriptionType.SYNCING,
        Subscription.class,
        syncingSubscriptions -> {
          if (syncStatus.inSync()) {
            if (!lastMessageWasInSync.orElse(Boolean.FALSE)) {
              syncingSubscriptions.forEach(
                  s ->
                      subscriptionManager.sendMessage(
                          s.getSubscriptionId(), new NotSynchronisingResult()));
              lastMessageWasInSync = Optional.of(Boolean.TRUE);
            }
          } else {
            if (lastMessageWasInSync.orElse(Boolean.TRUE)) {
              syncingSubscriptions.forEach(
                  s ->
                      subscriptionManager.sendMessage(
                          s.getSubscriptionId(), new SyncingResult(syncStatus)));
              lastMessageWasInSync = Optional.of(Boolean.FALSE);
            }
          }
        });
  }
}
