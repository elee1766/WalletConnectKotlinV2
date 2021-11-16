package org.walletconnect.walletconnectv2.relay.data

import com.tinder.scarlet.Stream
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import kotlinx.coroutines.flow.Flow
import org.walletconnect.walletconnectv2.relay.data.model.Relay

interface RelayService {

    @Receive
    fun observeEvents(): Stream<WebSocket.Event>

    @Receive
    fun eventsFlow(): Flow<WebSocket.Event>

    @Send
    fun publishRequest(publishRequest: Relay.Publish.Request)

    @Receive
    fun observePublishAcknowledgement(): Flow<Relay.Publish.Acknowledgement>

    @Receive
    fun observePublishError(): Flow<Relay.Publish.JsonRpcError>

    @Send
    fun subscribeRequest(subscribeRequest: Relay.Subscribe.Request)

    @Receive
    fun observeSubscribeAcknowledgement(): Flow<Relay.Subscribe.Acknowledgement>

    @Receive
    fun observeSubscribeError(): Flow<Relay.Subscribe.JsonRpcError>

    @Receive
    fun observeSubscriptionRequest(): Flow<Relay.Subscription.Request>

    @Send
    fun publishSubscriptionAcknowledgement(publishRequest: Relay.Subscription.Acknowledgement)

    @Send
    fun unsubscribeRequest(unsubscribeRequest: Relay.Unsubscribe.Request)

    @Receive
    fun observeUnsubscribeAcknowledgement(): Flow<Relay.Unsubscribe.Acknowledgement>

    @Receive
    fun observeUnsubscribeError(): Flow<Relay.Unsubscribe.JsonRpcError>
}