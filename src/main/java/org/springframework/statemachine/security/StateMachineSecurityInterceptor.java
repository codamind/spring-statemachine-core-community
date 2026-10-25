/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.statemachine.security;

import java.util.Collection;
import java.util.function.Supplier;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelCompilerMode;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.messaging.Message;
import org.springframework.security.access.expression.ExpressionUtils;
import org.springframework.security.access.expression.SecurityExpressionHandler;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.support.StateMachineInterceptor;
import org.springframework.statemachine.support.StateMachineInterceptorAdapter;
import org.springframework.statemachine.transition.Transition;
import org.springframework.util.StringUtils;

/**
 * {@link StateMachineInterceptor} which can be registered into a {@link StateMachine}
 * order to intercept a various security related checks.
 *
 * @author Janne Valkealahti
 *
 * @param <S> the type of state
 * @param <E> the type of event
 */
public class StateMachineSecurityInterceptor<S, E> extends StateMachineInterceptorAdapter<S, E> {

	private AuthorizationManager<Transition<S, E>> transitionAuthorizationManager;
	private AuthorizationManager<Message<E>> eventAuthorizationManager;
	private final ExpressionParser expressionParser = new SpelExpressionParser(new SpelParserConfiguration(SpelCompilerMode.OFF, null));
	private final SecurityExpressionHandler<Transition<?, ?>> transitionExpressionHandler =
			new DefaultTransitionSecurityExpressionHandler();
	private final SecurityExpressionHandler<Message<E>> eventExpressionHandler =
			new DefaultEventSecurityExpressionHandler<>();
	private SecurityRule eventSecurityRule;

	/**
	 * Instantiates a new state machine security interceptor.
	 */
	public StateMachineSecurityInterceptor() {
		this(null, null);
	}

	/**
	 * Instantiates a new state machine security interceptor with
	 * a custom {@link AuthorizationManager} for both transitions
	 * and events.
	 *
	 * @param transitionAuthorizationManager the transition authorization manager
	 * @param eventAuthorizationManager the event authorization manager
	 */
	public StateMachineSecurityInterceptor(AuthorizationManager<Transition<S, E>> transitionAuthorizationManager,
			AuthorizationManager<Message<E>> eventAuthorizationManager) {
		this(transitionAuthorizationManager, eventAuthorizationManager, null);
	}

	/**
	 * Instantiates a new state machine security interceptor with
	 * a custom {@link AuthorizationManager} for both transitions
	 * and events and a {@link SecurityRule} for events;
	 *
	 * @param transitionAuthorizationManager the transition authorization manager
	 * @param eventAuthorizationManager the event authorization manager
	 * @param eventSecurityRule the event security rule
	 */
	public StateMachineSecurityInterceptor(AuthorizationManager<Transition<S, E>> transitionAuthorizationManager,
			AuthorizationManager<Message<E>> eventAuthorizationManager, SecurityRule eventSecurityRule) {
		this.transitionAuthorizationManager = transitionAuthorizationManager;
		this.eventAuthorizationManager = eventAuthorizationManager;
		this.eventSecurityRule = eventSecurityRule;
	}

	@Override
	public Message<E> preEvent(Message<E> message, StateMachine<S, E> stateMachine) {
		if (eventSecurityRule != null) {
			decide(eventSecurityRule, message);
		}
		return super.preEvent(message, stateMachine);
	}

	@Override
	public StateContext<S, E> preTransition(StateContext<S, E> stateContext) {
		Transition<S, E> transition = stateContext.getTransition();
		SecurityRule rule = transition.getSecurityRule();
		if (rule != null) {
			decide(rule, transition);
		}
		return super.preTransition(stateContext);
	}

	/**
	 * Sets the event authorization manager.
	 *
	 * @param eventAuthorizationManager the new event authorization manager
	 */
	public void setEventAuthorizationManager(AuthorizationManager<Message<E>> eventAuthorizationManager) {
		this.eventAuthorizationManager = eventAuthorizationManager;
	}

	/**
	 * Sets the transition authorization manager.
	 *
	 * @param transitionAuthorizationManager the new transition authorization manager
	 */
	public void setTransitionAuthorizationManager(AuthorizationManager<Transition<S, E>> transitionAuthorizationManager) {
		this.transitionAuthorizationManager = transitionAuthorizationManager;
	}

	/**
	 * Sets the event security rule.
	 *
	 * @param eventSecurityRule the new event security rule
	 */
	public void setEventSecurityRule(SecurityRule eventSecurityRule) {
		this.eventSecurityRule = eventSecurityRule;
	}

	private void decide(SecurityRule rule, Message<E> object) {
		AuthorizationManager<Message<E>> manager = eventAuthorizationManager != null
				? eventAuthorizationManager : createDefaultEventManager(rule);
		decide(manager, object);
	}

	private void decide(SecurityRule rule, Transition<S, E> object) {
		AuthorizationManager<Transition<S, E>> manager = transitionAuthorizationManager != null
				? transitionAuthorizationManager : createDefaultTransitionManager(rule);
		decide(manager, object);
	}

	private <T> void decide(AuthorizationManager<T> manager, T object) {
		Supplier<Authentication> authentication = () -> SecurityContextHolder.getContext().getAuthentication();
		manager.verify(authentication, object);
	}

	private AuthorizationManager<Transition<S, E>> createDefaultTransitionManager(SecurityRule rule) {
		return (authentication, transition) -> decide(rule, authentication.get(), transition);
	}

	private AuthorizationManager<Message<E>> createDefaultEventManager(SecurityRule rule) {
		return (authentication, message) -> decide(rule, authentication.get(), message);
	}

	private AuthorizationDecision decide(SecurityRule rule, Authentication authentication, Transition<S, E> transition) {
		EvaluationContext context = transitionExpressionHandler.createEvaluationContext(authentication, transition);
		Boolean expression = StringUtils.hasText(rule.getExpression())
				? ExpressionUtils.evaluateAsBoolean(expressionParser.parseExpression(rule.getExpression()), context) : null;
		Boolean transitionAttribute = null;
		if (hasTransitionAttributes(rule.getAttributes())) {
			transitionAttribute = false;
			String source = transition.getSource().getId().toString();
			String target = transition.getTarget().getId().toString();
			for (String attribute : rule.getAttributes()) {
				if ((attribute.startsWith("TRANSITION_SOURCE_") && attribute.equals("TRANSITION_SOURCE_" + source))
						|| (attribute.startsWith("TRANSITION_TARGET_") && attribute.equals("TRANSITION_TARGET_" + target))) {
					transitionAttribute = true;
					break;
				}
			}
		}
		Boolean role = hasRole(authentication, rule.getAttributes());
		return decision(rule.getComparisonType(), expression, transitionAttribute, role);
	}

	private AuthorizationDecision decide(SecurityRule rule, Authentication authentication, Message<E> message) {
		EvaluationContext context = eventExpressionHandler.createEvaluationContext(authentication, message);
		Boolean expression = StringUtils.hasText(rule.getExpression())
				? ExpressionUtils.evaluateAsBoolean(expressionParser.parseExpression(rule.getExpression()), context) : null;
		Boolean eventAttribute = null;
		if (hasEventAttributes(rule.getAttributes())) {
			eventAttribute = false;
			String event = message.getPayload().toString();
			for (String attribute : rule.getAttributes()) {
				if (attribute.startsWith("EVENT_") && attribute.equals("EVENT_" + event)) {
					eventAttribute = true;
					break;
				}
			}
		}
		Boolean role = hasRole(authentication, rule.getAttributes());
		return decision(rule.getComparisonType(), expression, eventAttribute, role);
	}

	private boolean hasTransitionAttributes(Collection<String> attributes) {
		return attributes != null && attributes.stream().anyMatch(attribute ->
				attribute.startsWith("TRANSITION_SOURCE_") || attribute.startsWith("TRANSITION_TARGET_"));
	}

	private boolean hasEventAttributes(Collection<String> attributes) {
		return attributes != null && attributes.stream().anyMatch(attribute -> attribute.startsWith("EVENT_"));
	}

	private Boolean hasRole(Authentication authentication, Collection<String> attributes) {
		if (attributes == null || attributes.stream().noneMatch(attribute -> attribute.startsWith("ROLE_"))) {
			return null;
		}
		return authentication != null && authentication.getAuthorities().stream()
				.anyMatch(authority -> attributes.contains(authority.getAuthority()));
	}

	private AuthorizationDecision decision(SecurityRule.ComparisonType comparisonType, Boolean... votes) {
		int grants = 0;
		int denials = 0;
		for (Boolean vote : votes) {
			if (Boolean.TRUE.equals(vote)) {
				grants++;
			} else if (Boolean.FALSE.equals(vote)) {
				denials++;
			}
		}
		boolean granted;
		if (comparisonType == SecurityRule.ComparisonType.ANY) {
			granted = grants > 0;
		} else if (comparisonType == SecurityRule.ComparisonType.ALL) {
			granted = denials == 0 && grants > 0;
		} else if (comparisonType == SecurityRule.ComparisonType.MAJORITY) {
			granted = grants > denials;
		} else {
			throw new IllegalStateException("Unknown SecurityRule match type: " + comparisonType);
		}
		return new AuthorizationDecision(granted);
	}

	@Override
	public String toString() {
		return "StateMachineSecurityInterceptor [transitionAuthorizationManager=" + transitionAuthorizationManager
				+ ", eventAuthorizationManager=" + eventAuthorizationManager + ", eventSecurityRule=" + eventSecurityRule + "]";
	}

}
