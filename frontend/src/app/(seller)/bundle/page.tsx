"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/store/auth.store";
import { bundlesApi, categoriesApi } from "@/lib/api/api";
import type { Category } from "@/lib/api/types";

export default function CreateBundlePage() {
  const { user } = useAuth();
  const router = useRouter();
  const [categories, setCategories] = useState<Category[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // form fields
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [pickupDate, setPickupDate] = useState("");
  const [pickupStartTime, setPickupStartTime] = useState("");
  const [pickupEndTime, setPickupEndTime] = useState("");
  const [quantity, setQuantity] = useState(1);
  const [priceGbp, setPriceGbp] = useState("");
  const [discountPct, setDiscountPct] = useState(0);
  const [allergensText, setAllergensText] = useState("");
  const [activate, setActivate] = useState(true);

  useEffect(() => {
    categoriesApi.list().then(setCategories).catch(() => {});
  }, []);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!user?.token) return;

    setError("");
    setSuccess("");

    // validation
    if (!title.trim()) { setError("Title is required."); return; }
    if (!pickupDate || !pickupStartTime || !pickupEndTime) {
      setError("Pickup date and times are required."); return;
    }
    if (quantity < 1) { setError("Quantity must be at least 1."); return; }

    const price = parseFloat(priceGbp);
    if (isNaN(price) || price < 0) { setError("Enter a valid price."); return; }

    // build pickup timestamps
    const pickupStartAt = new Date(`${pickupDate}T${pickupStartTime}`).toISOString();
    const pickupEndAt = new Date(`${pickupDate}T${pickupEndTime}`).toISOString();

    if (pickupEndAt <= pickupStartAt) {
      setError("Pickup end time must be after start time."); return;
    }

    setSubmitting(true);
    try {
      await bundlesApi.create({
        title: title.trim(),
        description: description.trim() || undefined,
        categoryId: categoryId || undefined,
        pickupStartAt,
        pickupEndAt,
        quantityTotal: quantity,
        priceCents: Math.round(price * 100),
        discountPct: discountPct || undefined,
        allergensText: allergensText.trim() || undefined,
        activate,
      }, user.token);

      setSuccess("Bundle created successfully!");
      // reset form
      setTitle("");
      setDescription("");
      setCategoryId("");
      setPickupDate("");
      setPickupStartTime("");
      setPickupEndTime("");
      setQuantity(1);
      setPriceGbp("");
      setDiscountPct(0);
      setAllergensText("");
    } catch {
      setError("Failed to create bundle. Please try again.");
    } finally {
      setSubmitting(false);
    }
  }

  if (!user || user.role !== "SELLER") {
    return (
      <div className="page">
        <div className="card text-center py-16">
          <h1 className="text-4xl font-bold mb-4">Create Bundle</h1>
          <p className="text-muted">Please log in as a seller to create bundles.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="page" style={{ maxWidth: "40rem", margin: "0 auto" }}>
      <div className="mb-6">
        <h1 className="page-title">Create Bundle</h1>
        <p className="page-subtitle">Post surplus food for organisations to reserve.</p>
      </div>

      {error && <div className="alert alert-error mb-4">{error}</div>}
      {success && (
        <div className="card mb-4" style={{ backgroundColor: "#f0fdf4", border: "1px solid #bbf7d0", padding: "1rem" }}>
          <p style={{ color: "#16a34a", fontWeight: 500 }}>{success}</p>
          <div style={{ display: "flex", gap: "0.75rem", marginTop: "0.5rem" }}>
            <button className="btn btn-primary" style={{ fontSize: "0.85rem" }} onClick={() => setSuccess("")}>
              Create Another
            </button>
            <button className="btn btn-secondary" style={{ fontSize: "0.85rem" }} onClick={() => router.push("/dashboard")}>
              Back to Dashboard
            </button>
          </div>
        </div>
      )}

      {!success && (
        <form onSubmit={handleSubmit} className="card" style={{ display: "flex", flexDirection: "column", gap: "1.25rem" }}>
          {/* Title */}
          <div>
            <label style={{ display: "block", fontWeight: 500, marginBottom: "0.25rem", fontSize: "0.9rem" }}>
              Title *
            </label>
            <input
              className="input"
              type="text"
              placeholder="e.g. Mixed Bakery Bag"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              required
            />
          </div>

          {/* Description */}
          <div>
            <label style={{ display: "block", fontWeight: 500, marginBottom: "0.25rem", fontSize: "0.9rem" }}>
              Description
            </label>
            <textarea
              className="input"
              placeholder="What's included? How many items typically?"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={3}
              style={{ resize: "vertical" }}
            />
          </div>

          {/* Category */}
          <div>
            <label style={{ display: "block", fontWeight: 500, marginBottom: "0.25rem", fontSize: "0.9rem" }}>
              Category
            </label>
            <select
              className="input"
              value={categoryId}
              onChange={(e) => setCategoryId(e.target.value)}
            >
              <option value="">Select a category</option>
              {categories.map((c) => (
                <option key={c.categoryId} value={c.categoryId}>{c.name}</option>
              ))}
            </select>
          </div>

          {/* Pickup date + times */}
          <div>
            <label style={{ display: "block", fontWeight: 500, marginBottom: "0.25rem", fontSize: "0.9rem" }}>
              Pickup Date *
            </label>
            <input
              className="input"
              type="date"
              value={pickupDate}
              onChange={(e) => setPickupDate(e.target.value)}
              required
            />
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1rem" }}>
            <div>
              <label style={{ display: "block", fontWeight: 500, marginBottom: "0.25rem", fontSize: "0.9rem" }}>
                Pickup Start *
              </label>
              <input
                className="input"
                type="time"
                value={pickupStartTime}
                onChange={(e) => setPickupStartTime(e.target.value)}
                required
              />
            </div>
            <div>
              <label style={{ display: "block", fontWeight: 500, marginBottom: "0.25rem", fontSize: "0.9rem" }}>
                Pickup End *
              </label>
              <input
                className="input"
                type="time"
                value={pickupEndTime}
                onChange={(e) => setPickupEndTime(e.target.value)}
                required
              />
            </div>
          </div>

          {/* Quantity + Price */}
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1rem" }}>
            <div>
              <label style={{ display: "block", fontWeight: 500, marginBottom: "0.25rem", fontSize: "0.9rem" }}>
                Quantity *
              </label>
              <input
                className="input"
                type="number"
                min={1}
                value={quantity}
                onChange={(e) => setQuantity(parseInt(e.target.value) || 1)}
                required
              />
            </div>
            <div>
              <label style={{ display: "block", fontWeight: 500, marginBottom: "0.25rem", fontSize: "0.9rem" }}>
                Price (GBP) *
              </label>
              <input
                className="input"
                type="number"
                step="0.01"
                min="0"
                placeholder="3.50"
                value={priceGbp}
                onChange={(e) => setPriceGbp(e.target.value)}
                required
              />
            </div>
          </div>

          {/* Discount */}
          <div>
            <label style={{ display: "block", fontWeight: 500, marginBottom: "0.25rem", fontSize: "0.9rem" }}>
              Discount (%)
            </label>
            <input
              className="input"
              type="number"
              min={0}
              max={100}
              value={discountPct}
              onChange={(e) => setDiscountPct(parseInt(e.target.value) || 0)}
            />
            <p className="text-muted" style={{ fontSize: "0.8rem", marginTop: "0.25rem" }}>
              How much off the original price this bundle represents.
            </p>
          </div>

          {/* Allergens */}
          <div>
            <label style={{ display: "block", fontWeight: 500, marginBottom: "0.25rem", fontSize: "0.9rem" }}>
              Allergen Information
            </label>
            <input
              className="input"
              type="text"
              placeholder="e.g. gluten, dairy, eggs, nuts"
              value={allergensText}
              onChange={(e) => setAllergensText(e.target.value)}
            />
            <p className="text-muted" style={{ fontSize: "0.8rem", marginTop: "0.25rem" }}>
              List any allergens present in this bundle. This is shown to organisations before they reserve.
            </p>
          </div>

          {/* Activate toggle */}
          <label style={{ display: "flex", alignItems: "center", gap: "0.5rem", cursor: "pointer" }}>
            <input
              type="checkbox"
              checked={activate}
              onChange={(e) => setActivate(e.target.checked)}
            />
            <span style={{ fontSize: "0.9rem" }}>Make active immediately</span>
          </label>
          <p className="text-muted" style={{ fontSize: "0.8rem", marginTop: "-0.75rem" }}>
            {activate ? "Bundle will be visible to organisations right away." : "Bundle will be saved as a draft. You can activate it later."}
          </p>

          {/* Submit */}
          <button
            type="submit"
            className="btn btn-primary"
            disabled={submitting}
            style={{ marginTop: "0.5rem" }}
          >
            {submitting ? "Creating..." : activate ? "Post Bundle" : "Save as Draft"}
          </button>
        </form>
      )}
    </div>
  );
}
