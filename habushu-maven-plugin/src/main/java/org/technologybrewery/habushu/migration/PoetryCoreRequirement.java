package org.technologybrewery.habushu.migration;

import com.vdurmont.semver4j.Range;
import com.vdurmont.semver4j.Requirement;
import org.technologybrewery.habushu.HabushuException;
import org.technologybrewery.habushu.util.TomlUtils;

import java.lang.reflect.Field;

public class PoetryCoreRequirement extends Requirement {
    protected PoetryCoreRequirement(Range range, Requirement req1, RequirementOperator op, Requirement req2) {
        super(range, req1, op, req2);
    }

    public String getLowerBound() {
        String lowerBound = TomlUtils.DEFAULT_LOWER_BOUND;
        if (isABoundedRange()) {
            TomlUtils.VersionParts parts = TomlUtils.parseVersionConstraint(req1.toString());
            return padAndAdjustSemVer(parts);
        } else if (isAnUnboundedRange()) {
            TomlUtils.VersionParts parts = TomlUtils.parseVersionConstraint(range.toString());
            return padAndAdjustSemVer(parts);
        }
        return lowerBound;
    }

    public String getUpperBound() {
        String upperBound = null;
        if (isABoundedRange()) {
            TomlUtils.VersionParts parts = TomlUtils.parseVersionConstraint(req2.toString());
            upperBound = padAndAdjustSemVer(parts);
        } else if (isAnUnboundedRange()) {
            TomlUtils.VersionParts parts = TomlUtils.parseVersionConstraint(range.toString());
            upperBound = padAndAdjustSemVer(parts);
        }
        return upperBound;
    }

    public static PoetryCoreRequirement buildHabushu(String requirement) {
        String usedRequirement = requirement;
        if (requirement.contains(TomlUtils.COMMA)) {
            usedRequirement = requirement.replace(TomlUtils.COMMA,"");
        }

        return buildNPMReflection(usedRequirement);
    }

    public boolean isEncompassedBy(PoetryCoreRequirement incomingRequirement) {
        String incomingLowerBound = incomingRequirement.getLowerBound();
        String incomingUpperBound = incomingRequirement.getUpperBound();

        if (incomingUpperBound == null) {
            return isSatisfiedBy(incomingLowerBound);
        } else {
            return isSatisfiedBy(incomingLowerBound) && isSatisfiedBy(incomingUpperBound);
        }

    }

    private static PoetryCoreRequirement buildNPMReflection(String constraints) {
        try {
            // Create an instance of the Requirement class
            Requirement requirement = buildNPM(constraints);

            // Access the 'range' field
            Field rangeField = Requirement.class.getDeclaredField("range");
            rangeField.setAccessible(true); // Bypass Java access control checks
            Object rangeValue = rangeField.get(requirement);

            // Access the 'req1' field
            Field req1Field = Requirement.class.getDeclaredField("req1");
            req1Field.setAccessible(true);
            Object req1Value = req1Field.get(requirement);

            // Access the 'op' field
            Field opField = Requirement.class.getDeclaredField("op");
            opField.setAccessible(true);
            Object opValue = opField.get(requirement);

            // Access the 'req2' field
            Field req2Field = Requirement.class.getDeclaredField("req2");
            req2Field.setAccessible(true);
            Object req2Value = req2Field.get(requirement);

            return new PoetryCoreRequirement(
                    (Range) rangeValue,
                    (Requirement) req1Value,
                    (RequirementOperator) opValue,
                    (Requirement) req2Value
            );

        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new HabushuException("Unable to parse given Poetry Core required version constraints");
        }
    }

    private static String padAndAdjustSemVer(TomlUtils.VersionParts parts) {
        String operator = parts.getOperator();
        if (operator.contains(TomlUtils.EQUALS)) {
            String paddedVersion = TomlUtils.formatSemVerString(parts.getVersion());
            return paddedVersion;
        } else {
            String paddedVersion = TomlUtils.formatSemVerString(parts.getVersion());
            String nextVersion = paddedVersion;
            if (TomlUtils.GREATER_THAN.equals(operator)) {
                nextVersion = TomlUtils.incrementSemVersion(paddedVersion);
            } else if (TomlUtils.LESS_THAN.equals(operator)) {
                nextVersion = TomlUtils.decrementSemVersion(paddedVersion);
            }
            return nextVersion;
        }
    }

    private boolean isAnUnboundedRange() {
        return range != null;
    }

    private boolean isABoundedRange() {
        return req1 != null && req2 != null;
    }
}
