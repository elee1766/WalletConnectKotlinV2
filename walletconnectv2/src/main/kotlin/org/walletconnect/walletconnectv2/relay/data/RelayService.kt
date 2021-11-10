package org.walletconnect.walletconnectv2.relay.data

import com.squareup.moshi.JsonReader
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import org.walletconnect.walletconnectv2.relay.data.jsonrpc.PublishResponse
import org.walletconnect.walletconnectv2.relay.data.model.Relay

interface RelayService {

    @Receive
    fun eventsFlow(): Flow<WebSocket.Event>

    @Send
    fun publishRequest(publishRequest: Relay.Publish.Request)

    @Receive
    fun observePublishResponse(): Flow<JSONObject>//Flow<PublishResponse>////Relay.Publish.Response> //relay error or success response

    @Send
    fun subscribeRequest(subscribeRequest: Relay.Subscribe.Request)

    @Receive
    fun observeSubscribeResponse(): Flow<Relay.Subscribe.Response> //relay error or success response

    @Receive
    fun observeSubscriptionRequest(): Flow<Relay.Subscription.Request> //protocol error or success response

    @Send
    fun publishSubscriptionResponse(publishRequest: Relay.Subscription.Response)

    @Send
    fun subscriptionResponse(subscriptionResponse: Relay.Subscription.Response)

    @Send
    fun unsubscribeRequest(unsubscribeRequest: Relay.Unsubscribe.Request)

    @Receive
    fun observeUnsubscribeResponse(): Flow<Relay.Unsubscribe.Response> //relay error or success response
}