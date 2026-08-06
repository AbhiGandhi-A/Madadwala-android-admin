'use client';
import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { useEffect, useState } from 'react';

// Enhanced custom icon with Profile Image and Status Glow
const createProfileMarker = (color, imageUrl) => {
  const image = imageUrl || 'https://via.placeholder.com/40';
  return new L.DivIcon({
    html: `
      <div style="position: relative; width: 42px; height: 42px;">
        <div style="
          width: 42px;
          height: 42px;
          border-radius: 50%;
          border: 3px solid white;
          box-shadow: 0 4px 15px rgba(0,0,0,0.15);
          overflow: hidden;
          background: #f1f5f9;
        ">
          <img src="${image}" style="width: 100%; height: 100%; object-fit: cover;" onerror="this.src='https://via.placeholder.com/40'" />
        </div>
        <div style="
          position: absolute;
          bottom: 0;
          right: 0;
          width: 14px;
          height: 14px;
          background-color: ${color};
          border: 3px solid white;
          border-radius: 50%;
          box-shadow: 0 0 10px ${color};
          z-index: 10;
        "></div>
        <div style="
          position: absolute;
          top: 100%;
          left: 50%;
          transform: translateX(-50%);
          width: 0;
          height: 0;
          border-left: 8px solid transparent;
          border-right: 8px solid transparent;
          border-top: 10px solid white;
          margin-top: -4px;
        "></div>
      </div>
    `,
    className: 'custom-profile-marker',
    iconSize: [42, 52],
    iconAnchor: [21, 52],
  });
};

const RecenterMap = ({ lat, lng }) => {
  const map = useMap();
  useEffect(() => {
    if (lat && lng) {
      map.flyTo([lat, lng], 16, { animate: true, duration: 1.5 });
    }
  }, [lat, lng, map]);
  return null;
};

// Component to fix "white map" issue by invalidating size after initial render
const MapResizeFix = () => {
  const map = useMap();
  useEffect(() => {
    setTimeout(() => {
      map.invalidateSize();
    }, 250);
  }, [map]);
  return null;
};

export default function MapComponent({ partners = [], selectedPartner }) {
  // Default to Surat coordinates if no partner is selected
  const defaultCenter = [21.1702, 72.8311];
  const center = selectedPartner?.lat ? [selectedPartner.lat, selectedPartner.lng] : defaultCenter;

  return (
    <div className="w-full h-full bg-slate-50">
      <MapContainer
        center={center}
        zoom={13}
        style={{ height: '100%', width: '100%' }}
        zoomControl={false}
        scrollWheelZoom={true}
      >
        <TileLayer
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        />

        {Array.isArray(partners) && partners.map((p) => {
          if (!p.lat || !p.lng || isNaN(p.lat) || isNaN(p.lng)) return null;

          const statusColor = p.status === 'online' ? '#10b981' : p.status === 'busy' ? '#4f46e5' : '#94a3b8';

          return (
            <Marker
              key={p.uid}
              position={[p.lat, p.lng]}
              icon={createProfileMarker(statusColor, p.profileImage)}
            >
              <Popup offset={[0, -40]}>
                <div className="p-1 min-w-[100px] text-center font-sans">
                  <p className="font-bold text-slate-800 uppercase text-[10px] tracking-tight mb-1">{p.name}</p>
                  <div className="flex items-center justify-center gap-1.5">
                    <div className="w-1.5 h-1.5 rounded-full" style={{ backgroundColor: statusColor }}></div>
                    <p className="text-[8px] font-bold text-slate-500 uppercase">{p.status}</p>
                  </div>
                </div>
              </Popup>
            </Marker>
          );
        })}

        <RecenterMap lat={selectedPartner?.lat} lng={selectedPartner?.lng} />
        <MapResizeFix />
      </MapContainer>
    </div>
  );
}
