'use client';
import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { useEffect } from 'react';

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
          box-shadow: 0 8px 20px rgba(0,0,0,0.2);
          overflow: hidden;
          background: #f1f5f9;
        ">
          <img src="${image}" style="width: 100%; height: 100%; object-fit: cover;" />
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
      map.setView([lat, lng], 16, { animate: true, duration: 1 });
    }
  }, [lat, lng, map]);
  return null;
};

export default function MapComponent({ partners = [], selectedPartner }) {
  const center = selectedPartner?.lat ? [selectedPartner.lat, selectedPartner.lng] : [21.1702, 72.8311];

  return (
    <div style={{ height: '100%', width: '100%', background: '#f8fafc' }}>
      <MapContainer
        center={center}
        zoom={13}
        style={{ height: '100%', width: '100%' }}
        zoomControl={false}
        scrollWheelZoom={true}
      >
        <TileLayer
          url="https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png"
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>'
        />

        {Array.isArray(partners) && partners.map((p) => {
          if (!p.lat || !p.lng) return null;

          const statusColor = p.status === 'online' ? '#10b981' : p.status === 'busy' ? '#4f46e5' : '#94a3b8';

          return (
            <Marker
              key={p.uid}
              position={[p.lat, p.lng]}
              icon={createProfileMarker(statusColor, p.profileImage)}
            >
              <Popup offset={[0, -40]} className="custom-map-popup">
                <div className="p-2 min-w-[120px] text-center">
                  <p className="font-black text-slate-900 uppercase text-[11px] tracking-tight mb-1 italic">{p.name}</p>
                  <span className={`px-2 py-0.5 rounded text-[8px] font-black uppercase ${p.status === 'online' ? 'bg-emerald-50 text-emerald-600' : 'bg-indigo-50 text-indigo-600'}`}>{p.status}</span>
                </div>
              </Popup>
            </Marker>
          );
        })}

        <RecenterMap lat={selectedPartner?.lat} lng={selectedPartner?.lng} />
      </MapContainer>
    </div>
  );
}
