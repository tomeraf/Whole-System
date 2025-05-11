package com.halilovindustries.backend.Domain.Rules;

import java.util.Arrays;

public class RuleBuilder {
    public static Rule and(Rule... rules) {
        return (user, item, quantity) ->
            Arrays.stream(rules).allMatch(rule -> rule.evaluate(user, item, quantity));
    }

    public static Rule or(Rule... rules) {
        return (user, item, quantity) ->
            Arrays.stream(rules).anyMatch(rule -> rule.evaluate(user, item, quantity));
    }

    public static Rule ifThen(Rule condition, Rule consequence) {
        return (user, item, quantity) ->
            !condition.evaluate(user, item, quantity) || consequence.evaluate(user, item, quantity);
    }
}

