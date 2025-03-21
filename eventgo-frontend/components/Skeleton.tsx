export function SkeletonBox({ width = "full", height = 4, className = "" }: { width?: string | number; height?: string | number; className?: string }) {
	const w = typeof width === "number" ? `${width}px` : width;
	const h = typeof height === "number" ? `${height}px` : height;

	return <div className={`bg-gray-200 rounded ${className}`} style={{ width: w, height: h }} />;
}
