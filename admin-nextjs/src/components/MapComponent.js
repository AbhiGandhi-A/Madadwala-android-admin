'use client';
import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { useEffect } from 'react';

// Elite Marker: Profile Image with High-Contrast Border and Status Dot
const createEliteMarker = (status, imageUrl) => {
  const color = status === 'online' ? '#10b981' : status === 'busy' ? '#6366f1' : '#94a3b8';
  const img = imageUrl || 'https://via.placeholder.com/40';

  return new L.DivIcon({
    html: `
      <div style="position: relative; width: 48px; height: 48px;">
        <div style="
          width: 48px;
          height: 48px;
          border-radius: 50%;
          border: 4px solid white;
          box-shadow: 0 10px 25px rgba(0,0,0,0.2);
          overflow: hidden;
          background: #fff;
        ">
          <img src="${img}" style="width: 100%; height: 100%; object-fit: cover;" onerror="this.src='https://via.placeholder.com/40'" />
        </div>
        <div style="
          position: absolute;
          bottom: 2px;
          right: 2px;
          width: 14px;
          height: 14px;
          background-color: ${color};
          border: 2px solid white;
          border-radius: 50%;
          z-index: 20;
          box-shadow: 0 0 10px ${color};
        "></div>
        <div style="
          position: absolute;
          top: 100%;
          left: 50%;
          transform: translateX(-50%);
          width: 0;
          height: 0;
          border-left: 10px solid transparent;
          border-right: 10px solid transparent;
          border-top: 12px solid white;
          margin-top: -6px;
          filter: drop-shadow(0 4px 4px rgba(0,0,0,0.1));
        "></div>
      </div>
    `,
    className: 'elite-marker-icon',
    iconSize: [48, 60],
    iconAnchor: [24, 60],
  });
};

// Component to handle dynamic map centering and zooming
const MapController = ({ target }) => {
  const map = useMap();
  useEffect(() => {
    if (target?.lat && target?.lng) {
      map.flyTo([target.lat, target.lng], 16, {
        animate: true,
        duration: 1.5,
        easeLinearity: 0.25
      });
    }
  }, [target, map]);

  useEffect(() => {
    // Initial size fix for Leaflet in Next.js/React
    setTimeout(() => {
        map.invalidateSize();
    }, 400);
  }, [map]);

  return null;
};

export default function MapComponent({ partners = [], selectedPartner }) {
  // Default center point (India/Surat area)
  const defaultPos = [21.1702, 72.8311];

  return (
    <div className="w-full h-full bg-slate-50 font-sans">
      <MapContainer
        center={defaultPos}
        zoom={12}
        style={{ height: '100%', width: '100%' }}
        zoomControl={false}
      >
        {/* Google Maps Styled Professional Tiles (Unofficial) */}
        <TileLayer
          url="https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={z}"
          attribution='&copy; Google Maps'
        />

        {Array.isArray(partners) && partners.map((p) => {
          if (!p.lat || !p.lng || isNaN(p.lat) || isNaN(p.lng)) return null;

          return (
            <Marker
              key={p.uid}
              position={[p.lat, p.lng]}
              icon={createEliteMarker(p.status, p.profileImage)}
            >
              <Popup offset={[0, -50]}>
                <div className="p-1 text-center min-w-[100px]">
                  <p className="font-bold text-slate-900 uppercase text-[10px] tracking-tight mb-1">{p.name}</p>
                  <p className="text-[8px] font-bold text-indigo-500 uppercase tracking-widest">{p.status}</p>
                </div>
              </Popup>
            </Marker>
          );
        })}

        <MapController target={selectedPartner} />
      </MapContainer>
    </div>
  );
}
