package org.walletconnect.walletconnectv2.engine.sequence

import org.walletconnect.walletconnectv2.client.SessionProposal
import org.walletconnect.walletconnectv2.client.SessionRequest
import org.walletconnect.walletconnectv2.client.SettledSession

sealed class SequenceLifecycleEvent
class OnSessionProposal(val proposal: SessionProposal) : SequenceLifecycleEvent()
class OnSessionRequest(val request: SessionRequest) : SequenceLifecycleEvent()
class OnSessionDeleted(val topic: String, val reason: String) : SequenceLifecycleEvent()
object Unsupported : SequenceLifecycleEvent()
object Default : SequenceLifecycleEvent()