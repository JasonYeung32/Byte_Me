"use client";

import Link from "next/link";
import { useEffect, useState, useCallback } from "react";
import { useAuth } from "@/store/auth.store";
import { gamificationApi } from "@/lib/api/api";
import type { StatsResponse, StreakResponse } from "@/lib/api/types";

export default function ImpactPage() {
  const { user, init } = useAuth();
  const [stats, setStats] = useState<StatsResponse | null>(null);
  const [streak, setStreak] = useState<StreakResponse | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => { init(); }, [init]);

  const orgId = user?.role === "ORG_ADMIN" ? user.profileId : null;
  const token = user?.token;

  const loadStats = useCallback(async () => {
    if (!orgId || !token) return;
    setLoading(true);
    try {
      const [s, st] = await Promise.all([
        gamificationApi.stats(orgId, token),
        gamificationApi.streak(orgId, token),
      ]);
      setStats(s);
      setStreak(st);
    } catch {
      // silently fail for public page
    } finally {
      setLoading(false);
    }
  }, [orgId, token]);

  useEffect(() => { loadStats(); }, [loadStats]);

  return (
    <div className="page" style={{ maxWidth: "48rem", margin: "0 auto" }}>
      <div className="text-center mb-6">
        <h1 className="text-4xl font-bold mb-4" style={{ color: "var(--success-dark)" }}>Our Impact</h1>
        <p className="text-muted" style={{ fontSize: "1.1rem" }}>
          Every bundle rescued means less food wasted and fewer emissions.
        </p>
      </div>

      {/* Personal stats (org logged in) */}
      {orgId && stats && !loading && (
        <div className="card mb-6" style={{ padding: "2rem", backgroundColor: "#f0fdf4", border: "1px solid #bbf7d0" }}>
          <h2 style={{ fontSize: "1.25rem", fontWeight: 600, marginBottom: "1.5rem", color: "#166534", textAlign: "center" }}>
            Your Personal Impact
          </h2>
          <div className="grid grid-3" style={{ gap: "1rem" }}>
            <div style={{ textAlign: "center" }}>
              <p style={{ fontSize: "2.5rem", fontWeight: 700, color: "#166534" }}>{stats.mealsRescued}</p>
              <p className="text-muted">Meals Rescued</p>
            </div>
            <div style={{ textAlign: "center" }}>
              <p style={{ fontSize: "2.5rem", fontWeight: 700, color: "#166534" }}>
                {((stats.co2eSavedGrams) / 1000).toFixed(1)}
              </p>
              <p className="text-muted">kg CO2e Saved</p>
            </div>
            <div style={{ textAlign: "center" }}>
              <p style={{ fontSize: "2.5rem", fontWeight: 700, color: "#166534" }}>
                {streak?.currentStreakWeeks ?? 0}
              </p>
              <p className="text-muted">Week Streak</p>
            </div>
          </div>
          <div style={{ textAlign: "center", marginTop: "1.5rem" }}>
            <p className="text-muted" style={{ fontSize: "0.85rem" }}>
              {stats.totalReservations} total reservations, {stats.badgesEarned} badges earned
            </p>
            <Link href="/gamification" className="btn btn-primary" style={{ marginTop: "0.75rem" }}>
              View Achievements
            </Link>
          </div>
        </div>
      )}

      {loading && orgId && (
        <div className="card mb-6 text-center" style={{ padding: "2rem" }}>
          <p className="text-muted">Loading your stats...</p>
        </div>
      )}

      {/* How rescue helps */}
      <div className="card mb-6" style={{ padding: "2rem" }}>
        <h2 className="text-xl font-semibold mb-4">How Food Rescue Helps</h2>
        <div className="grid grid-3" style={{ gap: "1.5rem" }}>
          <div style={{ textAlign: "center" }}>
            <div style={{ fontSize: "2.5rem", marginBottom: "0.5rem" }}>1.5 kg</div>
            <p className="text-muted" style={{ fontSize: "0.9rem" }}>
              Average weight of food saved per bundle collected
            </p>
          </div>
          <div style={{ textAlign: "center" }}>
            <div style={{ fontSize: "2.5rem", marginBottom: "0.5rem" }}>2.5 kg</div>
            <p className="text-muted" style={{ fontSize: "0.9rem" }}>
              CO2 equivalent avoided per kg of food waste diverted from landfill (WRAP UK)
            </p>
          </div>
          <div style={{ textAlign: "center" }}>
            <div style={{ fontSize: "2.5rem", marginBottom: "0.5rem" }}>9.5M</div>
            <p className="text-muted" style={{ fontSize: "0.9rem" }}>
              Tonnes of food wasted annually in the UK alone
            </p>
          </div>
        </div>
      </div>

      {/* Mission */}
      <div className="card mb-6" style={{ padding: "2rem" }}>
        <h2 className="text-xl font-semibold mb-4">Our Mission</h2>
        <p style={{ lineHeight: 1.7 }}>
          Byte Me connects food sellers with charitable organisations to rescue surplus food
          that would otherwise go to waste. By making it easy to list, discover, and collect
          surplus bundles, we help reduce food waste, cut greenhouse gas emissions, and get
          quality food to people who need it.
        </p>
        <p style={{ lineHeight: 1.7, marginTop: "1rem" }}>
          Sellers use demand forecasting to optimise how much they post, reducing over-production.
          Organisations earn badges and maintain streaks to stay engaged with regular collections.
        </p>
      </div>

      {/* CTA for non-logged-in users */}
      {!user && (
        <div className="card text-center" style={{ padding: "2rem", backgroundColor: "var(--primary-light)" }}>
          <h2 className="text-xl font-semibold mb-4">Join the Movement</h2>
          <p className="text-muted mb-4">
            Sign up as a seller or organisation and start making an impact today.
          </p>
          <Link href="/register" className="btn btn-primary">Create Free Account</Link>
        </div>
      )}
    </div>
  );
}
