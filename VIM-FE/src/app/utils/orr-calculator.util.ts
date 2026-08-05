export type OrrIndustryCondition = 'expanding' | 'stable' | 'recessionary';
export type OrrFirmPosition = 'top_0_10' | 'top_11_30' | 'top_31_50' | 'range_51_80' | 'bottom_20';
export type OrrFinancingMode = 'normal' | 'seasonal';
export type OrrManagementRating = 'outstanding' | 'strong' | 'acceptable' | 'weak' | 'very_weak';
export type OrrOwnershipStructure = 'private_limited' | 'registered_partnership' | 'sole_proprietor_or_unregistered_partnership';
export type OrrPremisesOwnership = 'owned' | 'rented_or_leased';
export type OrrSourceOfFinancing = 'multiple' | 'single';
export type OrrRelationshipWithUbl = 'ten_plus_years' | 'seven_plus_years' | 'four_plus_years' | 'one_to_four_years' | 'new_client';
export type OrrDiversity = 'many' | 'few' | 'single';
export type OrrCibStatus = 'clean' | 'clean_with_restructure' | 'restructured' | 'overdue' | 'default';
export type OrrAuditedStatementQuality = 'audited_unqualified' | 'unaudited_or_qualified';
export type OrrAuditorCategory = 'A' | 'B' | 'C' | 'other';
export type OrrTrend = 'increasing' | 'stable' | 'decreasing';
export type OrrProfitTrend = 'increase_two_years' | 'increase_last_year' | 'other';

export interface OrrCalculationInput {
    parameter1: {
        industryCondition: OrrIndustryCondition;
    };
    parameter2: {
        firmPosition: OrrFirmPosition;
    };
    parameter3: {
        financingMode: OrrFinancingMode;
        fundBasedExposure: number;
        totalEquity: number;
    };
    parameter4: {
        dataSharingWithUbl: OrrManagementRating;
        technicalAndMarketKnowHow: OrrManagementRating;
        automatedMis: OrrManagementRating;
    };
    parameter5: {
        ownershipStructure: OrrOwnershipStructure;
    };
    parameter6: {
        businessPremisesOwnership: OrrPremisesOwnership;
    };
    parameter7: {
        otherSourcesOfFinancing: OrrSourceOfFinancing;
    };
    parameter8: {
        relationshipWithUbl: OrrRelationshipWithUbl;
    };
    parameter9: {
        yearsInBusiness: number;
    };
    parameter10: {
        buyersDiversity: OrrDiversity;
    };
    parameter11: {
        supplierDiversity: OrrDiversity;
    };
    parameter12: {
        hasDefaultOver30Days: boolean;
        cleanupRequirementMet: boolean;
        averageMarkupPaymentDays: number;
        cibStatus?: OrrCibStatus;
        cibScoreOverride?: number;
    };
    parameter13: {
        latestAuditedFinancialsUsed: boolean;
        statementQuality?: OrrAuditedStatementQuality;
        auditorCategory?: OrrAuditorCategory;
    };
    parameter14: {
        salesTrend: OrrTrend;
        netProfitTrend: OrrProfitTrend;
        currentAssets: number;
        currentLiabilities: number;
        totalLiabilities: number;
        equityWithoutSurplus: number;
        cashAvailableForDebtServicing: number;
        debtObligations: number;
        ebit: number;
        interestExpense: number;
        inventoryDays: number;
        receivableDays: number;
        payableDays: number;
        positiveOperatingCashFlowYearsOutOfThree: 0 | 1 | 2 | 3;
    };
    overallJudgementalOrrScore?: number | null;
    creditCommitteeAdjustedRiskRating?: number | null;
}

export interface OrrParameterBreakdown {
    parameter: number;
    name: string;
    score: number;
    maxScore: number;
    details?: Record<string, number | string | boolean | null>;
}

export interface OrrCalculationResult {
    totalScore: number;
    modelRiskRating: number;
    finalRiskRating: number;
    usedJudgementalOrr: boolean;
    breakdown: OrrParameterBreakdown[];
}

const MANAGEMENT_SCORE_MAP: Record<OrrManagementRating, number> = {
    outstanding: 2,
    strong: 1.5,
    acceptable: 1,
    weak: 0.5,
    very_weak: 0
};

const CIB_SCORE_MAP: Record<OrrCibStatus, number> = {
    clean: 6,
    clean_with_restructure: 3,
    restructured: 3,
    overdue: 0,
    default: 0
};

function assertFiniteNumber(value: number, label: string): void {
    if (!Number.isFinite(value)) {
        throw new Error(`${label} must be a finite number.`);
    }
}

function clampScore(score: number, maxScore: number): number {
    return Math.max(0, Math.min(maxScore, score));
}

function clampRiskRating(rating: number): number {
    return Math.max(1, Math.min(12, Math.round(rating)));
}

function safeDivide(numerator: number, denominator: number, label: string): number {
    assertFiniteNumber(numerator, `${label} numerator`);
    assertFiniteNumber(denominator, `${label} denominator`);
    if (denominator <= 0) {
        throw new Error(`${label} denominator must be greater than zero.`);
    }
    return numerator / denominator;
}

function roundScore(score: number): number {
    return Math.round(score * 100) / 100;
}

function getRiskRatingFromScore(score: number): number {
    if (score >= 95) {
        return 1;
    }
    if (score >= 87) {
        return 2;
    }
    if (score >= 79) {
        return 3;
    }
    if (score >= 71) {
        return 4;
    }
    if (score >= 63) {
        return 5;
    }
    if (score >= 55) {
        return 6;
    }
    if (score >= 47) {
        return 7;
    }
    if (score >= 39) {
        return 8;
    }
    if (score >= 30) {
        return 9;
    }
    if (score >= 20) {
        return 10;
    }
    if (score >= 10) {
        return 11;
    }
    return 12;
}

function calculateParameter1(input: OrrCalculationInput['parameter1']): OrrParameterBreakdown {
    const scoreMap: Record<OrrIndustryCondition, number> = {
        expanding: 6,
        stable: 3,
        recessionary: 0
    };
    return {
        parameter: 1,
        name: 'Condition of Industry',
        score: scoreMap[input.industryCondition],
        maxScore: 6
    };
}

function calculateParameter2(input: OrrCalculationInput['parameter2']): OrrParameterBreakdown {
    const scoreMap: Record<OrrFirmPosition, number> = {
        top_0_10: 3,
        top_11_30: 2.5,
        top_31_50: 2,
        range_51_80: 1,
        bottom_20: 0
    };
    return {
        parameter: 2,
        name: 'Firm Position in Industry',
        score: scoreMap[input.firmPosition],
        maxScore: 3
    };
}

function calculateParameter3(input: OrrCalculationInput['parameter3']): OrrParameterBreakdown {
    const ratio = safeDivide(input.fundBasedExposure, input.totalEquity, 'Borrowing / Equity');
    let score = 0;
    if (input.financingMode === 'seasonal') {
        if (ratio < 1) {
            score = 5;
        } else if (ratio <= 3) {
            score = 4;
        } else if (ratio <= 6) {
            score = 3;
        } else if (ratio <= 7) {
            score = 2;
        } else if (ratio <= 8) {
            score = 1;
        }
    } else {
        if (ratio < 0.75) {
            score = 5;
        } else if (ratio <= 1) {
            score = 4;
        } else if (ratio <= 2) {
            score = 3;
        } else if (ratio <= 3) {
            score = 2;
        } else if (ratio <= 4) {
            score = 1;
        }
    }

    return {
        parameter: 3,
        name: 'Borrowing / Equity',
        score,
        maxScore: 5,
        details: {
            ratio: roundScore(ratio)
        }
    };
}

function calculateParameter4(input: OrrCalculationInput['parameter4']): OrrParameterBreakdown {
    const score = MANAGEMENT_SCORE_MAP[input.dataSharingWithUbl]
        + MANAGEMENT_SCORE_MAP[input.technicalAndMarketKnowHow]
        + MANAGEMENT_SCORE_MAP[input.automatedMis];
    return {
        parameter: 4,
        name: 'Management Quality',
        score: clampScore(score, 8),
        maxScore: 8
    };
}

function calculateParameter5(input: OrrCalculationInput['parameter5']): OrrParameterBreakdown {
    const scoreMap: Record<OrrOwnershipStructure, number> = {
        private_limited: 6,
        registered_partnership: 3,
        sole_proprietor_or_unregistered_partnership: 0
    };
    return {
        parameter: 5,
        name: 'Ownership Structure',
        score: scoreMap[input.ownershipStructure],
        maxScore: 6
    };
}

function calculateParameter6(input: OrrCalculationInput['parameter6']): OrrParameterBreakdown {
    return {
        parameter: 6,
        name: 'Ownership of Business Premises',
        score: input.businessPremisesOwnership === 'owned' ? 5 : 0,
        maxScore: 5
    };
}

function calculateParameter7(input: OrrCalculationInput['parameter7']): OrrParameterBreakdown {
    return {
        parameter: 7,
        name: 'Other Sources of Financing',
        score: input.otherSourcesOfFinancing === 'multiple' ? 3 : 0,
        maxScore: 3
    };
}

function calculateParameter8(input: OrrCalculationInput['parameter8']): OrrParameterBreakdown {
    const scoreMap: Record<OrrRelationshipWithUbl, number> = {
        ten_plus_years: 10,
        seven_plus_years: 8,
        four_plus_years: 6,
        one_to_four_years: 4,
        new_client: 0
    };
    return {
        parameter: 8,
        name: 'Relationship with UBL',
        score: scoreMap[input.relationshipWithUbl],
        maxScore: 10
    };
}

function calculateParameter9(input: OrrCalculationInput['parameter9']): OrrParameterBreakdown {
    const years = input.yearsInBusiness;
    assertFiniteNumber(years, 'Years in business');
    let score = 0;
    if (years > 10) {
        score = 6;
    } else if (years >= 7) {
        score = 5;
    } else if (years >= 3) {
        score = 4;
    } else if (years >= 1) {
        score = 2;
    }
    return {
        parameter: 9,
        name: 'Years in Business',
        score,
        maxScore: 6
    };
}

function calculateParameter10(input: OrrCalculationInput['parameter10']): OrrParameterBreakdown {
    const scoreMap: Record<OrrDiversity, number> = {
        many: 2,
        few: 1,
        single: 0
    };
    return {
        parameter: 10,
        name: "Buyer's Diversity",
        score: scoreMap[input.buyersDiversity],
        maxScore: 2
    };
}

function calculateParameter11(input: OrrCalculationInput['parameter11']): OrrParameterBreakdown {
    const scoreMap: Record<OrrDiversity, number> = {
        many: 2,
        few: 1,
        single: 0
    };
    return {
        parameter: 11,
        name: 'Supplier Diversity',
        score: scoreMap[input.supplierDiversity],
        maxScore: 2
    };
}

function calculateParameter12(input: OrrCalculationInput['parameter12']): OrrParameterBreakdown {
    assertFiniteNumber(input.averageMarkupPaymentDays, 'Average markup payment days');
    const defaultHistoryScore = input.hasDefaultOver30Days ? 0 : 4;
    const cleanupScore = input.cleanupRequirementMet ? 2 : 0;
    const markupServicingScore = input.averageMarkupPaymentDays <= 15
        ? 2
        : input.averageMarkupPaymentDays <= 30
            ? 1
            : 0;
    const cibScore = input.cibScoreOverride != null
        ? clampScore(input.cibScoreOverride, 6)
        : CIB_SCORE_MAP[input.cibStatus || 'clean'];

    return {
        parameter: 12,
        name: 'Past Repayment / Relationship History',
        score: clampScore(defaultHistoryScore + cleanupScore + markupServicingScore + cibScore, 14),
        maxScore: 14,
        details: {
            defaultHistoryScore,
            cleanupScore,
            markupServicingScore,
            cibScore
        }
    };
}

function calculateParameter13(input: OrrCalculationInput['parameter13']): OrrParameterBreakdown {
    if (!input.latestAuditedFinancialsUsed) {
        return {
            parameter: 13,
            name: 'Quality of Audited Statements',
            score: 0,
            maxScore: 10,
            details: {
                latestAuditedFinancialsUsed: false
            }
        };
    }

    const statementScoreMap: Record<OrrAuditedStatementQuality, number> = {
        audited_unqualified: 5,
        unaudited_or_qualified: 0
    };
    const auditorScoreMap: Record<OrrAuditorCategory, number> = {
        A: 5,
        B: 3,
        C: 2,
        other: 0
    };

    if (!input.statementQuality || !input.auditorCategory) {
        throw new Error('Parameter 13 requires statementQuality and auditorCategory when latest audited financials are used.');
    }

    return {
        parameter: 13,
        name: 'Quality of Audited Statements',
        score: statementScoreMap[input.statementQuality] + auditorScoreMap[input.auditorCategory],
        maxScore: 10
    };
}

function calculateParameter14(input: OrrCalculationInput['parameter14']): OrrParameterBreakdown {
    const salesTrendScoreMap: Record<OrrTrend, number> = {
        increasing: 3,
        stable: 1,
        decreasing: 0
    };
    const netProfitTrendScoreMap: Record<OrrProfitTrend, number> = {
        increase_two_years: 2,
        increase_last_year: 1,
        other: 0
    };

    const currentRatio = safeDivide(input.currentAssets, input.currentLiabilities, 'Current ratio');
    const leverageRatio = safeDivide(input.totalLiabilities, input.equityWithoutSurplus, 'Leverage');
    const dscr = safeDivide(input.cashAvailableForDebtServicing, input.debtObligations, 'DSCR');
    const interestCoverageRatio = safeDivide(input.ebit, input.interestExpense, 'Interest coverage ratio');
    const netCashCycle = input.inventoryDays + input.receivableDays - input.payableDays;

    let currentRatioScore = 0;
    if (currentRatio >= 2) {
        currentRatioScore = 2;
    } else if (currentRatio >= 1) {
        currentRatioScore = 1;
    }

    let leverageScore = 0;
    if (leverageRatio < 0.75) {
        leverageScore = 5;
    } else if (leverageRatio < 1) {
        leverageScore = 4;
    } else if (leverageRatio < 2) {
        leverageScore = 3;
    } else if (leverageRatio < 3) {
        leverageScore = 2;
    }

    let dscrScore = 0;
    if (dscr >= 2.5) {
        dscrScore = 2;
    } else if (dscr >= 2) {
        dscrScore = 1;
    } else if (dscr >= 1.25) {
        dscrScore = 0.5;
    }

    let interestCoverageScore = 0;
    if (interestCoverageRatio >= 2) {
        interestCoverageScore = 2;
    } else if (interestCoverageRatio >= 1.5) {
        interestCoverageScore = 1.5;
    } else if (interestCoverageRatio >= 1.25) {
        interestCoverageScore = 1;
    }

    let netCashCycleScore = 0;
    if (netCashCycle <= 90) {
        netCashCycleScore = 2;
    } else if (netCashCycle <= 180) {
        netCashCycleScore = 1;
    }

    const netOperatingCashFlowScore = input.positiveOperatingCashFlowYearsOutOfThree >= 2
        ? input.positiveOperatingCashFlowYearsOutOfThree === 3
            ? 2
            : 1
        : 0;

    const score = salesTrendScoreMap[input.salesTrend]
        + netProfitTrendScoreMap[input.netProfitTrend]
        + currentRatioScore
        + leverageScore
        + dscrScore
        + interestCoverageScore
        + netCashCycleScore
        + netOperatingCashFlowScore;

    return {
        parameter: 14,
        name: 'Financial Performance',
        score: clampScore(score, 20),
        maxScore: 20,
        details: {
            currentRatio: roundScore(currentRatio),
            leverageRatio: roundScore(leverageRatio),
            dscr: roundScore(dscr),
            interestCoverageRatio: roundScore(interestCoverageRatio),
            netCashCycle: roundScore(netCashCycle)
        }
    };
}

export function calculateOrrAssessment(input: OrrCalculationInput): OrrCalculationResult {
    const parameter13RequiresJudgementalOrr = !input.parameter13.latestAuditedFinancialsUsed;

    if (parameter13RequiresJudgementalOrr && input.overallJudgementalOrrScore == null) {
        throw new Error('overallJudgementalOrrScore is required when latest audited financials are not used.');
    }

    const breakdown = [
        calculateParameter1(input.parameter1),
        calculateParameter2(input.parameter2),
        calculateParameter3(input.parameter3),
        calculateParameter4(input.parameter4),
        calculateParameter5(input.parameter5),
        calculateParameter6(input.parameter6),
        calculateParameter7(input.parameter7),
        calculateParameter8(input.parameter8),
        calculateParameter9(input.parameter9),
        calculateParameter10(input.parameter10),
        calculateParameter11(input.parameter11),
        calculateParameter12(input.parameter12),
        calculateParameter13(input.parameter13),
        calculateParameter14(input.parameter14)
    ];

    const modelTotalScore = roundScore(breakdown.reduce((sum, item) => sum + item.score, 0));
    const totalScore = parameter13RequiresJudgementalOrr
        ? (() => {
            const judgementalScore = Number(input.overallJudgementalOrrScore);
            assertFiniteNumber(judgementalScore, 'Overall judgemental ORR score');
            return clampScore(judgementalScore, 100);
        })()
        : modelTotalScore;
    const modelRiskRating = getRiskRatingFromScore(totalScore);
    const finalRiskRating = input.creditCommitteeAdjustedRiskRating != null
        ? clampRiskRating(Number(input.creditCommitteeAdjustedRiskRating))
        : modelRiskRating;

    return {
        totalScore: roundScore(totalScore),
        modelRiskRating,
        finalRiskRating,
        usedJudgementalOrr: parameter13RequiresJudgementalOrr,
        breakdown
    };
}
