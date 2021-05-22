package io.deckers.smtpjer.state_machines

import com.tinder.StateMachine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress

private fun sendStatus(code: Int, message: String, extendedCode: String? = null) =
  Command.WriteStatus(code, message, extendedCode)

private fun <S : State> StateMachine.GraphBuilder<State, Event, Command>.StateDefinitionBuilder<S>.onEscalation() {
  on<Event.OnParseError> {
    dontTransition(sendStatus(500, "Syntax error, command unrecognized"))
  }
  on<Event.OnQuit> {
    dontTransition(Command.Quit)
  }
}

class SmtpStateMachineParams {
  val onTransitionListeners = ArrayList<(StateMachine.Transition<State, Event, Command>) -> Unit>(emptyList())

  fun onTransition(listener: (StateMachine.Transition<State, Event, Command>) -> Unit) {
    onTransitionListeners.add(listener)
  }
}

class SmtpStateMachine(params: SmtpStateMachineParams) {
  private val stateMachine = StateMachine.create<State, Event, Command> {
    initialState(State.Start)

    state<State.Start> {
      on<Event.OnConnect> {
        transitionTo(State.Helo, sendStatus(220, "${InetAddress.getLocalHost()} Service ready"))
      }

      onEscalation()
    }

    state<State.Helo> {
      on<Event.OnEhlo> {
        transitionTo(State.Helo, sendStatus(500, "Syntax error, command unrecognized", "5.5.1"))
      }
      on<Event.OnHelo> {
        transitionTo(State.MailFrom(it.domain), sendStatus(250, "Ok"))
      }

      onEscalation()
    }

    state<State.MailFrom> {
      on<Event.OnMailFrom> {
        transitionTo(State.RcptTo(domain, it.emailAddress), sendStatus(250, "Ok", "2.1.0"))
      }

      onEscalation()
    }

    state<State.RcptTo> {
      on<Event.OnRcptTo> {
        transitionTo(
          State.Data(domain, mailFrom, it.emailAddress), sendStatus(250, "Ok", "2.1.5"),
        )
      }

      onEscalation()
    }

    state<State.Data> {
      on<Event.OnData> {
        transitionTo(State.Helo, Command.ReceiveData)
      }

      onEscalation()
    }

    params.onTransitionListeners.forEach(::onTransition)
  }

  suspend fun transition(event: Event) = withContext(Dispatchers.IO) {
    stateMachine.transition(event)
  }

  companion object {
    fun create(params: SmtpStateMachineParams.() -> Unit) =
      SmtpStateMachine(SmtpStateMachineParams().apply(params))
  }
}